"""
Light loader: read tasks from local PostgreSQL, write to BigQuery `tasks_raw`.

This is a stand-in for the Beam pipeline (dataflow/pipeline.py). It uses only
psycopg2 + the BigQuery client, so it runs cleanly inside Airflow without
apache-beam's heavy dependency tree. In production you'd instead have Airflow
submit the real Beam job to Dataflow (BeamRunPythonPipelineOperator).

WRITE_TRUNCATE makes this idempotent — re-running replaces the table instead of
appending duplicates, so Airflow retries are always safe.
"""

import os
import psycopg2
from google.cloud import bigquery

PROJECT = os.environ.get("GCP_PROJECT", "smartops-hub-2026")
DATASET = "smartops_analytics"
TABLE = f"{PROJECT}.{DATASET}.tasks_raw"

PG = dict(
    host=os.environ.get("PGHOST", "host.docker.internal"),
    port=int(os.environ.get("PGPORT", "5432")),
    dbname=os.environ.get("PGDATABASE", "smartops"),
    user=os.environ.get("PGUSER", "smartops"),
    password=os.environ.get("PGPASSWORD", "smartops123"),
)

SCHEMA = [
    bigquery.SchemaField("id", "INTEGER"),
    bigquery.SchemaField("title", "STRING"),
    bigquery.SchemaField("description", "STRING"),
    bigquery.SchemaField("priority", "STRING"),
    bigquery.SchemaField("status", "STRING"),
    bigquery.SchemaField("category", "STRING"),
    bigquery.SchemaField("ai_summary", "STRING"),
    bigquery.SchemaField("ai_analysed", "BOOLEAN"),
    bigquery.SchemaField("owner_name", "STRING"),
    bigquery.SchemaField("owner_email", "STRING"),
    bigquery.SchemaField("created_at", "TIMESTAMP"),
    bigquery.SchemaField("updated_at", "TIMESTAMP"),
]


def fetch_rows():
    conn = psycopg2.connect(**PG)
    cur = conn.cursor()
    cur.execute(
        """
        SELECT t.id, t.title, t.description, t.priority, t.status,
               t.category, t.ai_summary, t.ai_analysed,
               u.name, u.email, t.created_at, t.updated_at
        FROM tasks t
        JOIN users u ON t.user_id = u.id
        """
    )
    rows = cur.fetchall()
    cur.close()
    conn.close()
    return rows


def to_dict(r):
    return {
        "id": r[0],
        "title": r[1],
        "description": r[2],
        "priority": r[3],
        "status": r[4],
        "category": r[5],
        "ai_summary": r[6],
        "ai_analysed": r[7],
        "owner_name": r[8],
        "owner_email": r[9],
        "created_at": r[10].isoformat() if r[10] else None,
        "updated_at": r[11].isoformat() if r[11] else None,
    }


def main():
    rows = [to_dict(r) for r in fetch_rows()]

    client = bigquery.Client(project=PROJECT)
    job_config = bigquery.LoadJobConfig(
        schema=SCHEMA,
        write_disposition="WRITE_TRUNCATE",  # idempotent — safe to re-run
    )
    job = client.load_table_from_json(rows, TABLE, job_config=job_config)
    job.result()  # wait for the load to finish

    print(f"Loaded {len(rows)} rows into {TABLE}")


if __name__ == "__main__":
    main()
