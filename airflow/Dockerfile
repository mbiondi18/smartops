FROM apache/airflow:2.10.4

# Light loader dependencies — small, safe to install in Airflow's own env
RUN pip install --no-cache-dir psycopg2-binary "google-cloud-bigquery>=3.0"

# dbt in an ISOLATED virtualenv so its dependency tree never collides with
# Airflow's. This mirrors the production principle: keep each tool's
# environment separate. The DAG calls /opt/dbt-venv/bin/dbt directly.
# /opt is root-owned, so create the dir as root and hand it to the airflow user.
USER root
RUN mkdir -p /opt/dbt-venv && chown airflow: /opt/dbt-venv
USER airflow
RUN python -m venv /opt/dbt-venv \
 && /opt/dbt-venv/bin/pip install --no-cache-dir dbt-bigquery==1.11.3
