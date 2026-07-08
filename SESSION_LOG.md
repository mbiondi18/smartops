# SmartOps Hub — Learning Session Log

A running journal of each working/learning session: what we built, the questions asked (with short answers), and the plan for next time. Newest session at the top.

---

## Session — 2026-07-07

### What we did
- **SQL practice** — seeded 1000 tasks in local Postgres with `generate_series`; wrote 7 analytical queries (JOIN, GROUP BY/HAVING, conditional aggregation, window functions `ROW_NUMBER() OVER (PARTITION BY …)`, `DATE_TRUNC`, duration math).
- **BigQuery** — created the `smartops_analytics` dataset, loaded data via CSV, re-ran the same queries (skills transfer directly).
- **dbt** — built a project: `stg_tasks` (staging view, cleans data) → `task_metrics` (marts table, aggregates per user). Learned `source()` vs `ref()`, materialization (view vs table).
- **Apache Beam / Dataflow** — wrote `dataflow/pipeline.py` (Postgres → BigQuery); debugged `FILE_LOADS`→`STREAMING_INSERTS` and `WRITE_TRUNCATE`→`WRITE_APPEND`.
- **Looker Studio** — built a dashboard on `task_metrics` (bar, scorecard, pie).
- **Interview prep (concepts)** — warehouse vs lake, OLTP vs OLAP, BigQuery architecture, Airflow, Kafka/Pub-Sub, star schema (facts + dims), idempotency, batch vs streaming. Produced 3 artifacts: data-flow diagram, interview cheat sheet, data recap.
- **GCP account migration** — moved to new account `miguelbiondi18@gmail.com`, project `smartops-hub-2026` (fresh free trial). Updated Terraform + pipeline.py + dbt to the new project. Rebuilt infra with `terraform apply`.
- **Airflow** — stood up local Airflow in Docker (`airflow/`), DAG `smartops_data_pipeline`: `load_to_bigquery` (light loader) → `run_dbt`. dbt isolated in its own venv inside the image to avoid dependency conflicts. Ran it — both tasks green.
- **Full end-to-end trace** — created task **1001** via `POST /api/tasks` (user "Frank"), watched it travel: API → Postgres → Airflow → BigQuery `tasks_raw` → dbt → `task_metrics`.
- **Day-to-day DE tickets:**
  - DE-101: added dbt data quality tests (`unique`, `not_null`, `accepted_values`) to `stg_tasks`.
  - DE-102: added a new metric `high_priority_pct` to `task_metrics`.
  - DE-103: data quality incident — a `not_null` test on `category` failed (Frank's task 1001). Investigated → root cause is the API `CreateRequest` has no category field (it's set later by the AI job) → correct fix was `severity: warn`, not "make it pass".

### Questions I asked (with short answers)
- **Is the frontend Angular?** → No — Next.js 14 + React + TypeScript + Tailwind.
- **MCP / Skills / Hooks — what and how to configure?** → MCP = live tool access (e.g. Postgres/GitHub); Skills = saved slash-command procedures; Hooks = automation in settings.json.
- **Backend language? Spring Boot only Java?** → Java (Spring Boot). Spring Boot also supports Kotlin & Groovy (all JVM).
- **What is NestJS / Go?** → NestJS = Node/TypeScript backend framework (Spring-like); Go = compiled language, great for microservices.
- **Why not run analytics on Postgres directly?** → OLTP (Postgres, live app, fast small txns) vs OLAP (BigQuery, heavy scans). Analytics on prod would slow the app.
- **Is BigQuery just a bigger database?** → No — columnar, serverless, no indexes/keys, bills per bytes scanned. A different category (OLAP warehouse), not "bigger Postgres".
- **Is dbt just code?** → Yes — `.sql` + `.yml` in Git. dbt compiles SQL and sends it to BigQuery; the warehouse does the work.
- **What is Airflow?** → Orchestrator. DAGs (tasks + dependencies) in Python; schedules, retries, alerts. GCP managed = Cloud Composer.
- **How does data flow in a real env (Yummy)?** → Operational DB (Cloud SQL) → pipeline (Dataflow/Kafka) → BigQuery → dbt → dashboards; the two sides stay separate.
- **How does Airflow run Beam without dependency conflicts?** → It doesn't install Beam — it *triggers* the job (Dataflow / KubernetesPodOperator). Airflow orchestrates, it doesn't execute.
- **`.yml` vs `.sql` in dbt?** → `.sql` = transformation logic (builds data); `.yml` = config/tests/docs (describes/validates). YAML links to models by `name`, not location.
- **Always only staging & marts?** → No — convention. Common: staging → intermediate (optional) → marts, often split by domain.
- **How does dbt find schema.yml?** → `model-paths: ["models"]` + recursive scan; folder names only matter for config in `dbt_project.yml`.
- **Where does the compiled test SQL come from?** → dbt generic-test **macros** render SQL from your model/column into `target/compiled/…`.
- **Where's the operational data / Cloud SQL?** → In this setup it's local Docker Postgres (Cloud SQL is empty — the app never connects to it). Viewed via the app's admin panel / SQL clients / Cloud SQL Studio, not browsed like BigQuery.
- **How does the admin panel connect to Cloud SQL?** → It's another app with a DB datasource config pointing at the same Cloud SQL; staff view it in a browser.
- **How does CI/CD deploy my code?** → `.github/workflows/ci-cd.yml`: push to main → test → build & push images to Artifact Registry → `kubectl` rolling deploy to GKE → smoke test. (Env vars still point at the OLD project — needs updating for `smartops-hub-2026`.)
- **Terraform apply — is my code on GCP?** → No. Terraform built *empty* infra (GKE/Cloud SQL empty); code stays local until build→push→deploy.
- **PC restart — what keeps running?** → GCP keeps running (and billing) regardless of PC; local Docker stops (restart with `docker compose start`/`up -d`). Postgres data persists (named volume).
- **Where does "END TO END TEST TASK" come from?** → From our own `POST /api/tasks` body during the end-to-end trace — it's task 1001.

### Cost note
GCP infra (GKE + Cloud SQL) is idle but bills ~$2.50–5/day 24/7. Run `terraform destroy` when not deploying — local Docker + BigQuery are all we need for data work. BigQuery dataset is NOT managed by Terraform (created via `bq`), so destroy won't touch it.

### Tomorrow / next session
- **Build multiple microservices with role-based UIs** for **users**, **drivers**, and **admin** (mirrors Yummy architecture: services per domain + admin panel over the DB). Concepts to practice: microservices, database-per-service, API gateway, shared JWT auth, inter-service events (Pub/Sub).
- Continue **day-to-day DE tickets** (next up: incremental models, a new model, debugging a broken pipeline).
- Optionally resume the **mock interview** (paused at the star-schema question).

### Reminders
- I (Miguel) prefer to **run terminal commands myself** — Claude writes files and gives me the commands.
- Activate dbt venv: `terraform\environments\dev\dbt-env\Scripts\Activate.ps1`
- Airflow UI: http://localhost:8081 (admin password: `MSYS_NO_PATHCONV=1 docker exec smartops-airflow cat //opt/airflow/standalone_admin_password.txt`)

---

<!-- Template for future sessions:

## Session — YYYY-MM-DD
### What we did
### Questions I asked (with short answers)
### Tomorrow / next session
-->
