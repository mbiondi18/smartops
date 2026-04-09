# SmartOps Hub — Session Guide

Step-by-step instructions for starting and stopping the project each time you work on it.

---

## Before You Start (one-time setup, already done)

These are things you only do once. If you already did them, skip this section.

- Installed: Java 17, Maven, Docker Desktop, gcloud CLI, kubectl, Terraform, Python 3, Helm
- Created GCP project `smartops-hub-project`
- Created GCS bucket for Terraform state: `smartops-hub-project-tfstate`
- Created GitHub repo and set secrets `GCP_PROJECT_ID` and `GCP_SA_KEY`
- Ran `terraform apply` at least once

---

## Starting a Session

### Step 1 — Open the right terminal

Open **Google Cloud SDK Shell** from the Start menu. This is the only terminal where `gcloud` works.

Navigate to the project:
```
cd C:\Users\Miguel\Documents\Practice-Project\smartops-hub-phase1\smartops-hub
```

---

### Step 2 — Authenticate gcloud (if needed)

If you get authentication errors, run:
```
gcloud auth login
gcloud config set project smartops-hub-project
```

---

### Step 3 — Create the infrastructure with Terraform

> Skip this step if you did NOT destroy last time (resources still exist in GCP).

```
cd terraform/environments/dev
terraform init
terraform apply
```

Type `yes` when prompted. This takes about 10-15 minutes.

When it finishes, go back to the project root:
```
cd C:\Users\Miguel\Documents\Practice-Project\smartops-hub-phase1\smartops-hub
```

---

### Step 4 — Get the new Cloud SQL IP

> Do this every time after a `terraform apply`. The IP changes each time.

```
gcloud sql instances describe smartops-hub-project-dev-db --project=smartops-hub-project --format="value(ipAddresses[0].ipAddress)"
```

Copy the IP address from the output.

Open `k8s/configmap.yaml` and update the `SPRING_DATASOURCE_URL` line:
```yaml
SPRING_DATASOURCE_URL: jdbc:postgresql://<NEW-IP>:5432/smartops
```

---

### Step 5 — Allow Cloud SQL to accept connections from GKE

> Do this every time after a `terraform apply`.

```
gcloud sql instances patch smartops-hub-project-dev-db --authorized-networks=0.0.0.0/0 --project=smartops-hub-project --quiet
```

Wait for the command to finish (about 30 seconds).

---

### Step 6 — Connect kubectl to your GKE cluster

```
gcloud container clusters get-credentials smartops-hub-project-dev-gke --zone europe-west1-b --project smartops-hub-project
```

Verify it works:
```
kubectl get nodes
```

You should see one node listed as `Ready`.

---

### Step 7 — Apply Kubernetes manifests

```
kubectl apply -f k8s/
```

This creates/updates the deployment, service, configmap, HPA, and ServiceMonitor.

> Note: `k8s/secret.yaml` must already exist on your machine (it is gitignored). If it's missing, recreate it — see the "Recreating the Secret" section at the bottom.

---

### Step 8 — Install Prometheus + Grafana (Helm)

> Skip this step if you did NOT destroy last time (Helm release still exists).

```
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update
kubectl create namespace monitoring
helm install monitoring prometheus-community/kube-prometheus-stack -n monitoring
```

Wait about 2 minutes for all pods to start. Check they are ready:
```
kubectl get pods -n monitoring
```

All pods should show `Running` and `READY`.

---

### Step 9 — Apply all Kubernetes manifests including ServiceMonitor and alert rules

> Do this after every Helm install.

```
kubectl apply -f k8s/
```

---

### Step 10 — Recreate the Alertmanager config (after every Helm reinstall)

> The Alertmanager secret is not in git — you must recreate it manually each time.

```
kubectl create secret generic alertmanager-monitoring-kube-prometheus-alertmanager --from-file=alertmanager.yaml=alertmanager-config.yaml -n monitoring
kubectl rollout restart statefulset/alertmanager-monitoring-kube-prometheus-alertmanager -n monitoring
```

> Note: `alertmanager-config.yaml` must exist on your machine (it is gitignored). See "Recreating the Alertmanager Config" at the bottom if it's missing.

---

### Step 11 — Commit and push to trigger the pipeline

If you changed `configmap.yaml` (new IP), commit it:
```
git add k8s/configmap.yaml
git commit -m "fix: update Cloud SQL IP"
git push
```

This triggers the GitHub Actions pipeline. It will:
1. Run tests
2. Build and push a new Docker image
3. Deploy the new image to GKE

Watch the pipeline at: `https://github.com/mbiondi18/smartops-hub/actions`

Pipeline takes about 3-5 minutes.

---

### Step 12 — Verify the app is running

Check the pod status:
```
kubectl get pods
```

The pod should show `1/1 Running`. If it shows `CrashLoopBackOff` or `Error`, check the logs:
```
kubectl logs deployment/smartops-app --tail=50
```

Test the health endpoint in PowerShell:
```powershell
curl http://34.76.76.49/actuator/health -UseBasicParsing
```

Or use the Python script (from regular PowerShell):
```powershell
python scripts/health_check.py
```

---

### Step 13 — Access the app

| What | URL |
|------|-----|
| **Frontend (main UI)** | `http://34.77.92.49` |
| API base | `http://34.76.76.49/api/tasks` |
| Swagger UI (try endpoints) | `http://34.76.76.49/swagger-ui.html` |
| Health check | `http://34.76.76.49/actuator/health` |
| Raw metrics | `http://34.76.76.49/actuator/prometheus` |

> Note: The external IP (`34.76.76.49`) may change after a terraform destroy/apply. Check it with:
> ```
> kubectl get service smartops-service
> ```

---

### Step 14 — Access Grafana (optional)

Open a second terminal (Google Cloud SDK Shell) and run:
```
kubectl port-forward -n monitoring service/monitoring-grafana 3000:80
```

Leave it running. Open `http://localhost:3000` in your browser.

**Username:** `admin`

**Password:** Get it by running:
```
kubectl get secret -n monitoring monitoring-grafana -o jsonpath="{.data.admin-password}"
```

The output is base64-encoded. To decode it, paste the output at `https://www.base64decode.org` — or run this in PowerShell:
```powershell
[System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String("PASTE-OUTPUT-HERE"))
```

---

### Step 15 — Access Prometheus (optional)

Open another terminal and run:
```
kubectl port-forward -n monitoring service/monitoring-kube-prometheus-prometheus 9090:9090
```

Open `http://localhost:9090/targets` — you should see `smartops-service` with state `UP`.

---

## Stopping a Session

### Option A — Keep resources running (costs money overnight)

Just close your terminals. The app keeps running on GKE.

GCP will charge you for:
- The GKE node (~$0.05/hour)
- Cloud SQL (~$0.02/hour)
- Load Balancer (~$0.025/hour)

**Total: ~$2-3 per day** if left running.

---

### Option B — Destroy everything (recommended to save money)

> This deletes all GCP resources. Your code stays safe in Git.

**Step 1** — Destroy Terraform resources:
```
cd terraform/environments/dev
terraform destroy
```

Type `yes` when prompted. Takes 10-15 minutes.

> If you get an error about deleting the database user, it means Terraform is trying to delete the user before the database. Just run `terraform destroy` again — it usually succeeds on the second attempt.

**Step 2** — Verify everything is deleted in the GCP Console:
- Go to `https://console.cloud.google.com`
- Check: Kubernetes Engine → Clusters (should be empty)
- Check: SQL → Instances (should be empty)

**Step 3** — Close all terminals.

> Your Docker images in Artifact Registry are NOT deleted by terraform destroy. They stay there and cost a small amount (cents/month). Use `scripts/cleanup.py` to delete old ones.

---

## Common Problems and Fixes

### Pod is in CrashLoopBackOff

Check the logs:
```
kubectl logs deployment/smartops-app --tail=50
```

**If the error says "Connect timed out" or "Connection refused":**
The Cloud SQL IP in the ConfigMap is wrong, or Cloud SQL doesn't allow connections.

1. Check the current Cloud SQL IP:
   ```
   gcloud sql instances describe smartops-hub-project-dev-db --project=smartops-hub-project --format="value(ipAddresses[0].ipAddress)"
   ```
2. Update `k8s/configmap.yaml` with the correct IP
3. Run:
   ```
   kubectl apply -f k8s/configmap.yaml
   kubectl rollout restart deployment/smartops-app
   ```
4. Also re-run the authorized networks patch (Step 5 above)

---

### kubectl: "gke-gcloud-auth-plugin not found"

You're in the wrong terminal. Switch to the Google Cloud SDK Shell and run Step 6 again.

---

### Grafana login fails

The password changes every time Helm is reinstalled. Get the current password:
```
kubectl get secret -n monitoring monitoring-grafana -o jsonpath="{.data.admin-password}"
```

Then base64-decode the output.

---

### ServiceMonitor not showing in Prometheus targets

Wait 60 seconds after applying the ServiceMonitor, then refresh `http://localhost:9090/targets`.

If still missing:
```
kubectl get servicemonitor -n monitoring
kubectl apply -f k8s/servicemonitor.yaml
```

---

### Pipeline failing at "Wait for rollout"

The pod is not becoming healthy. Check:
```
kubectl get pods
kubectl logs deployment/smartops-app --tail=30
```

Most common cause: wrong database IP or Cloud SQL not allowing connections.

---

### External IP changed after terraform apply

```
kubectl get service smartops-service
```

The `EXTERNAL-IP` column shows the current IP. Update your bookmarks.

---

## Recreating the Alertmanager Config

`alertmanager-config.yaml` is gitignored. If it's missing, create it:

1. Create the file `alertmanager-config.yaml` in the project root with this content:
   ```yaml
   global:
     smtp_smarthost: 'smtp.gmail.com:587'
     smtp_from: 'mbiondi1188@gmail.com'
     smtp_auth_username: 'mbiondi1188@gmail.com'
     smtp_auth_password: 'YOUR-GMAIL-APP-PASSWORD'
     smtp_require_tls: true
   route:
     group_by: ['alertname']
     group_wait: 30s
     group_interval: 5m
     repeat_interval: 12h
     receiver: email
   receivers:
     - name: email
       email_configs:
         - to: 'mbiondi1188@gmail.com'
           send_resolved: true
   ```

2. Apply it:
   ```
   kubectl create secret generic alertmanager-monitoring-kube-prometheus-alertmanager --from-file=alertmanager.yaml=alertmanager-config.yaml -n monitoring
   kubectl rollout restart statefulset/alertmanager-monitoring-kube-prometheus-alertmanager -n monitoring
   ```

---

## Recreating the Secret

`k8s/secret.yaml` is gitignored and must exist on your machine. If it's missing, create it:

1. Base64-encode your values. In PowerShell:
   ```powershell
   [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("smartops"))        # username
   [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("your-db-password")) # password
   [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("your-anthropic-key")) # api key
   ```

2. Create `k8s/secret.yaml`:
   ```yaml
   apiVersion: v1
   kind: Secret
   metadata:
     name: smartops-secret
     namespace: default
   type: Opaque
   data:
     SPRING_DATASOURCE_USERNAME: <base64-username>
     SPRING_DATASOURCE_PASSWORD: <base64-password>
     ANTHROPIC_API_KEY: <base64-api-key>
   ```

3. Apply it:
   ```
   kubectl apply -f k8s/secret.yaml
   ```

---

## Quick Reference — Most Used Commands

```bash
# See running pods
kubectl get pods

# See app logs
kubectl logs deployment/smartops-app --tail=50

# Restart the app pod
kubectl rollout restart deployment/smartops-app

# Watch a deploy in progress
kubectl rollout status deployment/smartops-app

# See all services and their IPs
kubectl get services

# Apply all k8s changes
kubectl apply -f k8s/

# Run health check
python scripts/health_check.py

# Preview old images to delete (no deletes)
python scripts/cleanup.py --dry-run

# Trigger pipeline manually
$env:GITHUB_TOKEN = "your-token"
python scripts/deploy.py
```
