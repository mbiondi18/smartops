# SmartOps Hub — Full Project Documentation

## What is this project?

SmartOps Hub is a full-stack cloud-native task management application built as a portfolio project to demonstrate real-world software engineering skills across the full stack: backend development, frontend, authentication, cloud infrastructure, containerization, CI/CD automation, and observability.

Users register an account, log in, and manage their own tasks through a React UI. When you trigger an AI analysis on a task, it calls Claude (Anthropic's AI) to automatically summarize the task and suggest a priority and category for it.

---

## Architecture Overview

```
Browser / Swagger UI
        │
        ▼
  LoadBalancer (GCP)
  External IP: 34.76.76.49
        │
        ▼
  Kubernetes (GKE)
  ┌─────────────────────────────┐
  │  smartops-app Pod           │
  │  Spring Boot on port 8080   │
  │                             │
  │  ┌─────────────────────┐    │
  │  │  TaskController     │    │
  │  │  AiAnalysisService  │    │
  │  │  TaskService        │    │
  │  └─────────────────────┘    │
  └─────────────────────────────┘
        │                  │
        ▼                  ▼
  Cloud SQL           Claude API
  PostgreSQL        (Anthropic)
  (GCP)
        │
        ▼
  Prometheus (scrapes /actuator/prometheus every 15s)
        │
        ▼
  Grafana (dashboards)
```

---

## Tech Stack

| Layer | Technology | Why |
|-------|-----------|-----|
| Language | Java 17 | Stable LTS version with modern features |
| Framework | Spring Boot 3.2 | Industry standard for Java REST APIs |
| Auth | Spring Security + JWT | Stateless authentication — users log in and get a token |
| Database | PostgreSQL 15 | Reliable relational DB, strong GCP support |
| ORM | Hibernate / JPA | Maps Java objects to database tables automatically |
| HTTP Client | WebFlux WebClient | Non-blocking HTTP client for calling Claude API |
| Frontend | Next.js 14 + React | Full-stack React framework for the user interface |
| Styling | Tailwind CSS | Utility CSS classes for fast UI development |
| Container | Docker | Packages the app and all its dependencies into one image |
| Orchestration | Kubernetes (GKE) | Runs and manages containers in production |
| Infrastructure | Terraform | Defines all GCP resources as code |
| CI/CD | GitHub Actions | Automates test → build → deploy on every push |
| Registry | GCP Artifact Registry | Stores Docker images |
| Monitoring | Prometheus + Grafana | Collects and displays app metrics |
| Alerting | Alertmanager + Gmail | Sends email when something goes wrong |
| API Docs | Swagger / OpenAPI | Auto-generated interactive API documentation |
| AI | Claude Haiku (Anthropic) | Analyses tasks and suggests priority/category |

---

## Project Structure

```
smartops-hub/
├── backend/                          # Java Spring Boot application
│   ├── src/main/java/com/smartops/
│   │   ├── SmartOpsApplication.java  # App entry point
│   │   ├── controller/
│   │   │   ├── TaskController.java   # Task HTTP endpoints
│   │   │   └── AuthController.java   # Register/login endpoints
│   │   ├── service/
│   │   │   ├── TaskService.java      # Business logic for tasks
│   │   │   ├── UserService.java      # Register/login logic
│   │   │   └── AiAnalysisService.java# Claude API integration
│   │   ├── model/
│   │   │   ├── Task.java             # Task database entity
│   │   │   └── User.java             # User database entity
│   │   ├── dto/
│   │   │   ├── TaskDTO.java          # Task request/response shapes
│   │   │   └── AuthDTO.java          # Auth request/response shapes
│   │   ├── repository/
│   │   │   ├── TaskRepository.java   # Task database queries
│   │   │   └── UserRepository.java   # User database queries
│   │   └── config/
│   │       ├── SecurityConfig.java        # Spring Security rules
│   │       ├── JwtUtil.java               # JWT create/validate
│   │       ├── JwtAuthFilter.java         # JWT request interceptor
│   │       ├── CustomUserDetailsService.java # Load user from DB
│   │       ├── CorsConfig.java            # Allow frontend requests
│   │       └── GlobalExceptionHandler.java# Error handling
│   ├── src/main/resources/
│   │   └── application.yml           # App configuration
│   ├── src/test/                     # Unit and integration tests
│   ├── Dockerfile                    # How to build the Docker image
│   └── pom.xml                       # Maven dependencies
│
├── frontend/                         # Next.js React application
│   ├── app/
│   │   ├── layout.tsx                # Root layout
│   │   ├── page.tsx                  # Root redirect
│   │   ├── login/page.tsx            # Login page
│   │   ├── register/page.tsx         # Register page
│   │   └── dashboard/page.tsx        # Main task dashboard
│   ├── lib/
│   │   └── api.ts                    # Axios API client + types
│   ├── Dockerfile                    # How to build the frontend image
│   └── package.json                  # Node dependencies
│
├── k8s/                              # Kubernetes manifests
│   ├── deployment.yaml               # Backend pod
│   ├── service.yaml                  # Backend LoadBalancer
│   ├── frontend-deployment.yaml      # Frontend pod
│   ├── frontend-service.yaml         # Frontend LoadBalancer
│   ├── configmap.yaml                # Non-secret env vars
│   ├── secret.yaml                   # Sensitive values (gitignored)
│   ├── hpa.yaml                      # Auto-scaling rules
│   ├── servicemonitor.yaml           # Prometheus scrape config
│   └── prometheus-rules.yaml         # Alert rules
│
├── terraform/                        # Infrastructure as code
│   ├── modules/
│   │   ├── networking/               # VPC and subnet
│   │   ├── registry/                 # Artifact Registry
│   │   ├── database/                 # Cloud SQL PostgreSQL
│   │   └── kubernetes/               # GKE cluster
│   └── environments/
│       └── dev/                      # Dev environment wiring
│
├── scripts/                          # Python automation
│   ├── health_check.py               # Check if app is alive
│   ├── cleanup.py                    # Delete old Docker images
│   └── deploy.py                     # Trigger pipeline via GitHub API
│
├── .github/workflows/
│   └── ci-cd.yml                     # GitHub Actions pipeline
│
└── docker-compose.yml                # Run locally with Docker
```

---

## Phase-by-Phase Walkthrough

### Phase 1 — REST API (Spring Boot + PostgreSQL)

**What we built:** A standard CRUD REST API for tasks.

**Key concepts:**

- **Spring Boot** is a framework that makes it easy to build web servers in Java. Instead of writing hundreds of lines of boilerplate, you annotate your classes and Spring wires everything together.

- **REST API** means the app communicates over HTTP using standard verbs: GET (read), POST (create), PUT (update), DELETE (remove). Each URL is called an endpoint.

- **JPA / Hibernate** is a tool that automatically creates database tables from your Java classes and translates between Java objects and rows in the database. You write `task.save()` and it generates the SQL for you.

- **Lombok** is a library that auto-generates repetitive Java code (getters, setters, constructors) using annotations like `@Builder` and `@Data`.

**API Endpoints:**

| Method | URL | What it does |
|--------|-----|--------------|
| POST | `/api/tasks` | Create a new task |
| GET | `/api/tasks` | Get all tasks |
| GET | `/api/tasks/{id}` | Get one task by ID |
| PUT | `/api/tasks/{id}` | Update a task |
| DELETE | `/api/tasks/{id}` | Delete a task |
| POST | `/api/tasks/{id}/analyse` | Run AI analysis on a task |

**Task model fields:**

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Auto-generated unique identifier |
| title | String | Task title (required) |
| description | String | Task details |
| status | Enum | TODO, IN_PROGRESS, DONE |
| priority | Enum | LOW, MEDIUM, HIGH |
| category | Enum | WORK, PERSONAL, URGENT, OTHER |
| aiAnalysed | Boolean | Whether Claude has analysed this task |
| createdAt | Timestamp | Auto-set on creation |

**Swagger UI:** Available at `http://<ip>/swagger-ui.html` — an interactive page where you can try every API endpoint in the browser without writing any code.

---

### Phase 2 — AI Integration (Claude API)

**What we built:** `AiAnalysisService.java` — calls Claude to analyse tasks.

**Key concepts:**

- **WebClient** is Spring's HTTP client for calling external APIs. It's non-blocking, meaning the app doesn't freeze while waiting for the response.

- **Claude API** (Anthropic) is called at `https://api.anthropic.com/v1/messages`. You send a prompt and get a text response back. We use `claude-haiku-4-5-20251001` — the fast, cost-efficient model.

- **`@PostConstruct`** runs `init()` automatically when the app starts, to build the WebClient with the API key from the environment.

- **`@Scheduled`** runs `analyseUnprocessedTasks()` every hour automatically to process tasks that haven't been analysed yet.

**How a task analysis works:**
1. You call `POST /api/tasks/1/analyse`
2. The controller calls `AiAnalysisService.analyseTask(1)`
3. The service builds a prompt: *"Analyse this task, respond with JSON containing summary, priority, category"*
4. Sends the prompt to Claude API with `WebClient`
5. Parses the JSON response from Claude
6. Returns `AnalysisResponse` with the summary and suggestions
7. The controller saves the result back to the task in the database

**Configuration (application.yml):**
```yaml
anthropic:
  api:
    key: ${ANTHROPIC_API_KEY:placeholder}   # loaded from Kubernetes Secret
    enabled: ${ANTHROPIC_API_ENABLED:false} # loaded from ConfigMap
    model: ${ANTHROPIC_MODEL:claude-haiku-4-5-20251001}
```

The `${VAR:default}` syntax means: read from environment variable, fall back to default if not set.

---

### Phase 3 — Docker

**What we built:** `backend/Dockerfile` to containerize the app.

**What Docker does:** It packages your app plus everything it needs (Java, dependencies, config) into a single file called an **image**. Anyone with Docker can run that image and get the exact same app, regardless of what OS they have.

**Multi-stage build:**
- **Stage 1 (build):** Uses a Maven image to compile the Java code and create a `.jar` file
- **Stage 2 (runtime):** Uses a minimal Java image and copies only the `.jar` — the final image is much smaller because it doesn't include Maven or source code

**docker-compose.yml:** For running locally. Starts both the app and a local PostgreSQL database with one command: `docker compose up`. This is for development — in production we use Kubernetes.

---

### Phase 4 — Terraform (GCP Infrastructure)

**What we built:** Terraform code that creates all GCP resources automatically.

**What Terraform does:** Instead of clicking around the GCP console to create resources, you write code that describes what you want. Terraform figures out what needs to be created, modified, or deleted to reach that state. This is called **Infrastructure as Code (IaC)**.

**Resources created by Terraform:**

| Resource | What it is |
|----------|-----------|
| VPC + Subnet | Private network inside GCP for your resources |
| Artifact Registry | Private Docker image storage (like a private Docker Hub) |
| Cloud SQL (PostgreSQL) | Managed PostgreSQL database — GCP handles backups, patches |
| GKE Cluster | Managed Kubernetes — GCP runs the control plane |

**Remote state:** Terraform saves its state (what it created) in a GCS bucket (`smartops-hub-project-tfstate`). This means you can run Terraform from any machine and it knows what already exists.

**Modules:** Reusable pieces of Terraform code. The `networking`, `database`, `registry`, and `kubernetes` modules each manage one concern. The `environments/dev/main.tf` wires them all together.

**Key commands:**
```bash
terraform init      # download providers and modules
terraform plan      # preview what will be created/changed/destroyed
terraform apply     # actually create the resources
terraform destroy   # delete everything (use to save money when done)
```

> **Important:** Every `terraform destroy` + `apply` cycle gives Cloud SQL a new public IP. After every `apply` you must run:
> ```
> gcloud sql instances patch smartops-hub-project-dev-db --authorized-networks=0.0.0.0/0 --project=smartops-hub-project
> ```
> And update `k8s/configmap.yaml` with the new IP.

---

### Phase 5 — Kubernetes (GKE Deployment)

**What we built:** Kubernetes manifest files in `k8s/`.

**What Kubernetes does:** It runs your Docker containers in production. It automatically restarts crashed containers, handles rolling updates (zero-downtime deploys), and routes traffic to healthy pods.

**Manifest files explained:**

**`deployment.yaml`** — Tells Kubernetes how to run the app:
- How many copies (replicas) to run
- Which Docker image to use
- What environment variables to inject
- Health checks: `/actuator/health` — Kubernetes restarts the pod if this fails
- Rolling update strategy: `maxSurge: 0, maxUnavailable: 1` — for a single-node cluster (can't run 2 pods at once)

**`service.yaml`** — Exposes the app to the internet:
- Type `LoadBalancer` creates a GCP load balancer with a public IP
- Routes external traffic on port 80 → pod port 8080

**`configmap.yaml`** — Non-secret environment variables:
- Database URL, model name, feature flags
- Injected into the pod at startup

**`secret.yaml`** (gitignored) — Sensitive values:
- Database username/password, Anthropic API key
- Stored as base64 in Kubernetes, injected as environment variables

**`hpa.yaml`** — Horizontal Pod Autoscaler:
- Automatically adds pods when CPU > 70%
- Scales between 1 and 3 replicas

**Key commands:**
```bash
kubectl apply -f k8s/          # apply all manifests
kubectl get pods               # see running pods
kubectl logs deployment/smartops-app  # see app logs
kubectl rollout restart deployment/smartops-app  # restart pods
kubectl rollout status deployment/smartops-app   # watch a deploy
```

---

### Phase 6 — CI/CD Pipeline (GitHub Actions)

**What we built:** `.github/workflows/ci-cd.yml`

**What CI/CD does:** Every time you push code to `main`, GitHub automatically runs a pipeline that tests, builds, and deploys the app. You never manually deploy — the pipeline does it.

**Pipeline stages:**

```
Push to main
     │
     ▼
┌─────────────┐
│  Stage 1    │  Run unit tests (TaskServiceTest)
│   Test      │  Run integration tests (TaskControllerIntegrationTest)
└─────────────┘
     │ (must pass)
     ▼
┌─────────────┐
│  Stage 2    │  Authenticate to GCP
│  Build &    │  Build Docker image tagged with git commit SHA
│  Push Image │  Push to Artifact Registry
└─────────────┘
     │ (must succeed)
     ▼
┌─────────────┐
│  Stage 3    │  Get GKE credentials
│  Deploy     │  kubectl set image (swap old image for new one)
│  to GKE     │  Wait for rollout to complete
└─────────────┘
```

**GitHub Secrets used:**
- `GCP_PROJECT_ID` — your GCP project ID
- `GCP_SA_KEY` — GCP Service Account key JSON (allows GitHub to authenticate to GCP)

**Each pipeline run creates a new Docker image** tagged with the git commit SHA (a unique hash like `a3f9c2d...`). It also overwrites the `latest` tag. This means you can always roll back to any previous commit's image.

---

### Phase 7 — Observability (Prometheus + Grafana)

**What we built:** Metrics scraping and dashboards.

**What observability means:** Being able to see what your app is doing in production — memory usage, request rates, errors, database connections — without having to read logs line by line.

**How the metrics pipeline works:**

```
Spring Boot app
      │
      │  exposes metrics at /actuator/prometheus
      │  (format: jvm_memory_used_bytes 12345678)
      ▼
Prometheus
      │
      │  scrapes every 15 seconds
      │  stores time-series data
      ▼
Grafana
      │
      │  queries Prometheus
      │  renders graphs
      ▼
You (dashboard in browser)
```

**Micrometer** is the library that collects metrics inside Spring Boot and formats them for Prometheus. Added to `pom.xml`:
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

**ServiceMonitor** (`k8s/servicemonitor.yaml`) is a Kubernetes custom resource that tells Prometheus: *"scrape the service with label `app: smartops-app` at `/actuator/prometheus` every 15 seconds"*.

**Helm** is a package manager for Kubernetes. We used it to install `kube-prometheus-stack` — a bundle that includes Prometheus, Grafana, and Alertmanager already configured to work together:
```bash
helm install monitoring prometheus-community/kube-prometheus-stack -n monitoring
```

**Grafana Dashboard metrics:**

| Metric | PromQL Query | What it shows |
|--------|-------------|---------------|
| JVM Heap Memory | `jvm_memory_used_bytes{job="smartops-service", area="heap"}` | RAM used by the Java app |
| HTTP Request Rate | `rate(http_server_requests_seconds_count{job="smartops-service"}[1m])` | Requests per second |
| CPU Usage | `process_cpu_usage{job="smartops-service"}` | CPU % used by the process |
| DB Connections | `hikaricp_connections_active{job="smartops-service"}` | Active database connections |

**Accessing Grafana** (requires port-forward since it has no public IP):
```bash
kubectl port-forward -n monitoring service/monitoring-grafana 3000:80
# then open http://localhost:3000
# username: admin
# password: get with: kubectl get secret -n monitoring monitoring-grafana -o jsonpath="{.data.admin-password}"
# then base64-decode the output
```

---

### Phase 8 — Python Automation Scripts

**What we built:** Three scripts in `scripts/` to automate common operational tasks.

#### `health_check.py`

Hits the app's health endpoint and exits with code 1 if the app is down. Useful for:
- Cron jobs that alert you if the app goes down
- Quick sanity check after a deploy

```bash
python scripts/health_check.py
# Output: [2026-04-09 19:03:12 UTC] OK  — SmartOps Hub is UP
```

```bash
python scripts/health_check.py --url http://different-ip
```

#### `cleanup.py`

Lists all Docker images in Artifact Registry and deletes old ones, keeping the N most recent. Every pipeline run creates a new image — without cleanup, you accumulate images and pay for storage.

```bash
python scripts/cleanup.py --dry-run       # preview (no deletes)
python scripts/cleanup.py --keep 3        # keep only 3 most recent
python scripts/cleanup.py                 # keep 5 (default)
```

Must be run from the Google Cloud SDK Shell (needs `gcloud` in PATH).

#### `deploy.py`

Triggers the GitHub Actions pipeline via the GitHub API without pushing any code. Useful when you want to force a redeploy (e.g., after fixing infrastructure) without changing code.

```bash
$env:GITHUB_TOKEN = "your-github-personal-access-token"
python scripts/deploy.py
python scripts/deploy.py --branch main
```

---

---

### Phase A — Authentication (Spring Security + JWT)

**What we built:** Full user authentication — register, login, and protected endpoints.

**Key concepts:**

- **JWT (JSON Web Token)** is a small encrypted string that proves who you are. When you log in, the server generates one and gives it to you. On every future request you send it in the `Authorization: Bearer <token>` header. The server validates it without hitting the database again.

- **BCrypt** is a password hashing algorithm. When you register, your password is hashed (scrambled one-way) before being stored. The database never contains your real password — only a hash.

- **Spring Security** is the framework that protects endpoints. We configured it to: allow `/api/auth/**` publicly, and require a valid JWT for everything else.

- **`JwtAuthFilter`** runs on every request before it reaches the controller. It reads the token from the header, validates it, and tells Spring Security who the user is.

- **`CustomUserDetailsService`** loads a user from the database by email. Spring Security calls this internally.

**New endpoints:**

| Method | URL | What it does |
|--------|-----|--------------|
| POST | `/api/auth/register` | Create account, returns JWT token |
| POST | `/api/auth/login` | Login, returns JWT token |

**Tasks now belong to users** — you can only see and modify your own tasks.

---

### Phase B — Frontend (Next.js + React)

**What we built:** A full visual UI at `http://34.77.92.49`.

**Key concepts:**

- **Next.js** is a React framework. React builds the UI components, Next.js handles routing (different pages), building, and serving.

- **Tailwind CSS** lets you style components with utility classes directly in JSX: `className="text-blue-600 font-bold"` instead of writing separate CSS files.

- **Axios** is the HTTP client used to call the backend API from the browser. It has an interceptor that automatically adds the JWT token to every request.

- **localStorage** stores the JWT token in the browser so you stay logged in when you refresh the page.

**Pages:**
- `/login` — email + password form
- `/register` — name + email + password form
- `/dashboard` — task list + detail panel + create form

**How frontend talks to backend:** The browser at `http://34.77.92.49` makes HTTP requests to `http://34.76.76.49/api/...`. CORS (Cross-Origin Resource Sharing) is configured on the backend to allow this — without it the browser would block the requests.

**CI/CD:** The pipeline now builds two Docker images in parallel (backend + frontend) and deploys both.

---

### Phase D — Alerting (Prometheus Alertmanager + Gmail)

**What we built:** Automatic email alerts when something goes wrong.

**How it works:**
```
Prometheus detects condition (e.g. pod down)
        │
        ▼
Fires an alert → sends to Alertmanager
        │
        ▼
Alertmanager sends email to mbiondi1188@gmail.com
```

**Alert rules (`k8s/prometheus-rules.yaml`):**

| Alert | Condition | Severity |
|-------|-----------|----------|
| SmartOpsAppDown | No healthy pod for 1 minute | Critical |
| SmartOpsPodRestarting | Pod restarted 3+ times in 10 min | Warning |
| SmartOpsHighMemory | JVM heap > 400MB for 2 min | Warning |
| SmartOpsHighErrorRate | 5xx errors > 5% of requests | Warning |

**Alertmanager config** is stored as a Kubernetes Secret (not in git — contains Gmail app password). It uses Gmail SMTP on port 587 with TLS.

**Gmail App Password** — a special 16-character password generated specifically for this integration. Your real Gmail password is never used.

---

## Recurring Operations (Things You'll Need to Do Again)

### Starting a new session after terraform destroy

After `terraform destroy` + `terraform apply`, the Cloud SQL IP changes. You must:

1. Get the new IP:
   ```
   gcloud sql instances describe smartops-hub-project-dev-db --project=smartops-hub-project --format="value(ipAddresses[0].ipAddress)"
   ```

2. Update `k8s/configmap.yaml` with the new IP

3. Allow all connections to Cloud SQL:
   ```
   gcloud sql instances patch smartops-hub-project-dev-db --authorized-networks=0.0.0.0/0 --project=smartops-hub-project
   ```

4. Reinstall Helm (Prometheus + Grafana):
   ```
   helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
   helm repo update
   kubectl create namespace monitoring
   helm install monitoring prometheus-community/kube-prometheus-stack -n monitoring
   ```

5. Apply all Kubernetes manifests:
   ```
   kubectl apply -f k8s/
   ```

6. Commit and push (triggers pipeline):
   ```
   git add k8s/configmap.yaml
   git commit -m "fix: update Cloud SQL IP"
   git push
   ```

### Accessing the app

| What | URL |
|------|-----|
| **Frontend (main UI)** | `http://34.77.92.49` |
| REST API | `http://34.76.76.49/api/tasks` |
| Swagger UI | `http://34.76.76.49/swagger-ui.html` |
| Health check | `http://34.76.76.49/actuator/health` |
| Metrics (raw) | `http://34.76.76.49/actuator/prometheus` |
| Grafana | `http://localhost:3000` (after port-forward) |
| Prometheus | `http://localhost:9090` (after port-forward) |
| Prometheus Alerts | `http://localhost:9090/alerts` (after port-forward) |

### Port-forwarding monitoring tools

```bash
# Grafana
kubectl port-forward -n monitoring service/monitoring-grafana 3000:80

# Prometheus
kubectl port-forward -n monitoring service/monitoring-kube-prometheus-prometheus 9090:9090
```

---

## Security Notes

- `k8s/secret.yaml` is gitignored — never commit it
- The GCP Service Account key (`gha-key.json`) is gitignored — never commit it
- The Anthropic API key lives only in the Kubernetes Secret, never in code
- Cloud SQL authorized networks set to `0.0.0.0/0` is acceptable for dev, but in production you would restrict to the GKE cluster's IP range

---

## Cost Management

GCP charges for:
- GKE cluster (the nodes that run pods) — most expensive
- Cloud SQL instance — second most expensive
- Artifact Registry storage — cheap
- Load Balancer — small fixed cost

When you're not actively working on the project, run `terraform destroy` to avoid charges. All your code stays in Git — you can recreate everything with `terraform apply`.

---

## Glossary

| Term | Plain English |
|------|--------------|
| Pod | The smallest unit in Kubernetes — one running container |
| Node | A virtual machine that runs pods |
| Deployment | A Kubernetes resource that manages pods and handles rolling updates |
| Service | A Kubernetes resource that gives pods a stable IP/URL |
| ConfigMap | Key-value pairs injected into pods as environment variables |
| Secret | Like ConfigMap but for sensitive values — base64 encoded |
| HPA | Horizontal Pod Autoscaler — automatically adds/removes pods based on load |
| ServiceMonitor | Tells Prometheus which services to scrape |
| Helm | Package manager for Kubernetes — installs pre-configured bundles |
| Image | A packaged, runnable version of your app (like a snapshot) |
| Registry | Storage for Docker images |
| CI/CD | Continuous Integration / Continuous Deployment — automated build and release |
| PromQL | Prometheus Query Language — used to query metrics in Grafana |
| Actuator | Spring Boot module that exposes health, metrics, and info endpoints |
| Micrometer | Library that collects app metrics and exports them to Prometheus |
| WebClient | Spring's non-blocking HTTP client for calling external APIs |
| JPA | Java Persistence API — standard for ORM in Java |
| HikariCP | The connection pool Spring Boot uses for database connections |
