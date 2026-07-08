import apache_beam as beam
from apache_beam.options.pipeline_options import PipelineOptions, GoogleCloudOptions, StandardOptions
from apache_beam.io.gcp.bigquery import WriteToBigQuery, BigQueryDisposition
import psycopg2
import argparse


BQ_TABLE = "smartops-hub-2026:smartops_analytics.tasks_raw"

BQ_SCHEMA = {
    "fields": [
        {"name": "id",          "type": "INTEGER"},
        {"name": "title",       "type": "STRING"},
        {"name": "description", "type": "STRING"},
        {"name": "priority",    "type": "STRING"},
        {"name": "status",      "type": "STRING"},
        {"name": "category",    "type": "STRING"},
        {"name": "ai_summary",  "type": "STRING"},
        {"name": "ai_analysed", "type": "BOOLEAN"},
        {"name": "owner_name",  "type": "STRING"},
        {"name": "owner_email", "type": "STRING"},
        {"name": "created_at",  "type": "TIMESTAMP"},
        {"name": "updated_at",  "type": "TIMESTAMP"},
    ]
}


def read_from_postgres():
    conn = psycopg2.connect(
        host="localhost",
        port=5432,
        dbname="smartops",
        user="smartops",
        password="smartops123"
    )
    cur = conn.cursor()
    cur.execute("""
        SELECT
            t.id, t.title, t.description, t.priority, t.status,
            t.category, t.ai_summary, t.ai_analysed,
            u.name, u.email,
            t.created_at, t.updated_at
        FROM tasks t
        JOIN users u ON t.user_id = u.id
    """)
    rows = cur.fetchall()
    cur.close()
    conn.close()
    return rows


def row_to_dict(row):
    return {
        "id":          row[0],
        "title":       row[1],
        "description": row[2],
        "priority":    row[3],
        "status":      row[4],
        "category":    row[5],
        "ai_summary":  row[6],
        "ai_analysed": row[7],
        "owner_name":  row[8],
        "owner_email": row[9],
        "created_at":  row[10].isoformat() if row[10] else None,
        "updated_at":  row[11].isoformat() if row[11] else None,
    }


def run():
    options = PipelineOptions()

    with beam.Pipeline(options=options) as p:
        (
            p
            | "Read from PostgreSQL"  >> beam.Create(read_from_postgres())
            | "Convert to dict"       >> beam.Map(row_to_dict)
            | "Write to BigQuery"     >> WriteToBigQuery(
                table=BQ_TABLE,
                schema=BQ_SCHEMA,
                write_disposition=BigQueryDisposition.WRITE_APPEND,
                create_disposition=BigQueryDisposition.CREATE_IF_NEEDED,
                method="STREAMING_INSERTS",
            )
        )


if __name__ == "__main__":
    run()
