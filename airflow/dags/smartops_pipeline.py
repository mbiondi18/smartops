"""
SmartOps data pipeline DAG.

Orchestrates the two steps you previously ran by hand:
  1. load_to_bigquery — reads local PostgreSQL, writes to BigQuery (tasks_raw)
  2. run_dbt          — transforms raw data into staging + marts models

The `>>` at the bottom defines the order: dbt only runs after the load
succeeds, so it never transforms stale or missing data.
"""

from datetime import datetime
from airflow import DAG
from airflow.operators.bash import BashOperator

with DAG(
    dag_id="smartops_data_pipeline",
    description="Load Postgres into BigQuery, then transform with dbt",
    schedule="@hourly",              # run every hour
    start_date=datetime(2026, 7, 1),
    catchup=False,                   # don't backfill missed runs
    tags=["smartops", "data"],
) as dag:

    load_to_bigquery = BashOperator(
        task_id="load_to_bigquery",
        bash_command="python /opt/project/airflow/scripts/load_to_bigquery.py",
    )

    run_dbt = BashOperator(
        task_id="run_dbt",
        # dbt lives in its own isolated venv (see Dockerfile)
        bash_command=(
            "/opt/dbt-venv/bin/dbt run "
            "--project-dir /opt/project/smartops_dbt "
            "--profiles-dir /opt/project/airflow/dbt_profile"
        ),
    )

    # Dependency: load first, THEN transform
    load_to_bigquery >> run_dbt
