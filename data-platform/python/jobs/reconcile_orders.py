from __future__ import annotations

import json
import os
from datetime import datetime, timezone
from pathlib import Path

import boto3
import psycopg

def main() -> None:
    conninfo = (
        f"host={os.getenv('POSTGRES_HOST', 'localhost')} "
        f"port={os.getenv('POSTGRES_PORT', '5432')} "
        f"dbname={os.getenv('POSTGRES_DB', 'commerce')} "
        f"user={os.getenv('POSTGRES_USER', 'commerce')} "
        f"password={os.getenv('POSTGRES_PASSWORD', 'commerce')}"
    )
    with psycopg.connect(conninfo) as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT order_id FROM orders")
            source_orders = {row[0] for row in cur.fetchall()}
            cur.execute("SELECT entity_id FROM ingested_events WHERE event_type='ORDER_CREATED'")
            ingested_orders = {row[0] for row in cur.fetchall()}

    missing = sorted(source_orders - ingested_orders)
    unexpected = sorted(ingested_orders - source_orders)
    report = {
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "source_order_count": len(source_orders),
        "ingested_order_event_count": len(ingested_orders),
        "missing_order_events": missing,
        "unexpected_order_events": unexpected,
        "consistent": not missing and not unexpected,
    }

    output = Path("/app/reports/reconciliation.json")
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, indent=2, sort_keys=True))

    kwargs = {"region_name": os.getenv("AWS_REGION", "us-east-1")}
    if os.getenv("S3_ENDPOINT"):
        kwargs["endpoint_url"] = os.environ["S3_ENDPOINT"]
    s3 = boto3.client("s3", **kwargs)
    bucket = os.getenv("S3_BUCKET", "commerce-data-lake")
    stamp = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H-%M-%SZ")
    body = json.dumps(report, indent=2, sort_keys=True).encode()
    s3.put_object(Bucket=bucket, Key=f"quality/reconciliation/{stamp}.json", Body=body, ContentType="application/json")
    s3.put_object(Bucket=bucket, Key="quality/reconciliation/latest.json", Body=body, ContentType="application/json")
    print(json.dumps(report, indent=2, sort_keys=True))
    if not report["consistent"]:
        raise SystemExit(2)

if __name__ == "__main__":
    main()
