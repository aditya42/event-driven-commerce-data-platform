from __future__ import annotations

import json
import os
import shutil
import tempfile
from pathlib import Path

import boto3
from pyspark.sql import SparkSession, functions as F

ALLOWED = ["customer", "product", "inventory", "cart", "order", "payment", "shipment", "refund"]

def s3_client():
    kwargs = {"region_name": os.getenv("AWS_REGION", "us-east-1")}
    endpoint = os.getenv("S3_ENDPOINT")
    if endpoint:
        kwargs["endpoint_url"] = endpoint
    return boto3.client("s3", **kwargs)

def download_prefix(client, bucket: str, prefix: str, destination: Path) -> int:
    count = 0
    paginator = client.get_paginator("list_objects_v2")
    for page in paginator.paginate(Bucket=bucket, Prefix=prefix):
        for obj in page.get("Contents", []):
            key = obj["Key"]
            if key.endswith("/"):
                continue
            local = destination / key
            local.parent.mkdir(parents=True, exist_ok=True)
            client.download_file(bucket, key, str(local))
            count += 1
    return count

def upload_tree(client, bucket: str, source: Path, prefix: str) -> int:
    uploaded = 0
    for file in source.rglob("*"):
        if file.is_file() and not file.name.startswith("."):
            rel = file.relative_to(source).as_posix()
            client.upload_file(str(file), bucket, f"{prefix.rstrip('/')}/{rel}")
            uploaded += 1
    return uploaded

def main() -> None:
    bucket = os.getenv("S3_BUCKET", "commerce-data-lake")
    client = s3_client()
    work = Path(tempfile.mkdtemp(prefix="commerce-curation-"))
    raw_root = work / "raw"
    curated_root = work / "curated"
    quarantine_root = work / "quarantine"

    try:
        downloaded = download_prefix(client, bucket, "raw/", raw_root)
        if downloaded == 0:
            raise SystemExit("No raw events found. Run `make demo` first.")

        spark = (SparkSession.builder
                 .appName("commerce-event-curation")
                 .master(os.getenv("SPARK_MASTER", "local[*]"))
                 .config("spark.sql.session.timeZone", "UTC")
                 .getOrCreate())
        spark.sparkContext.setLogLevel("WARN")

        df = spark.read.option("recursiveFileLookup", "true").json(str(raw_root))
        required = ["eventId", "eventType", "entityType", "entityId", "occurredAt", "schemaVersion", "source", "payload"]
        for name in required:
            if name not in df.columns:
                df = df.withColumn(name, F.lit(None))

        normalized = df.select(
            F.col("eventId").cast("string").alias("event_id"),
            F.upper(F.col("eventType").cast("string")).alias("event_type"),
            F.lower(F.col("entityType").cast("string")).alias("entity_type"),
            F.col("entityId").cast("string").alias("entity_id"),
            F.to_timestamp("occurredAt").alias("occurred_at"),
            F.col("schemaVersion").cast("int").alias("schema_version"),
            F.col("source").cast("string").alias("source"),
            F.to_json("payload").alias("payload_json"),
        ).withColumn("event_date", F.to_date("occurred_at"))

        valid_expr = (
            F.col("event_id").isNotNull()
            & F.col("event_type").isNotNull()
            & F.col("entity_type").isin(ALLOWED)
            & F.col("entity_id").isNotNull()
            & F.col("occurred_at").isNotNull()
            & (F.col("schema_version") >= 1)
            & F.col("source").isNotNull()
            & F.col("payload_json").isNotNull()
        )

        valid = normalized.filter(valid_expr).dropDuplicates(["event_id"])
        invalid = normalized.filter(~valid_expr)

        valid_count = valid.count()
        invalid_count = invalid.count()
        duplicate_count = max(normalized.filter(valid_expr).count() - valid_count, 0)

        valid.write.mode("overwrite").partitionBy("entity_type", "event_date").parquet(str(curated_root))
        if invalid_count:
            invalid.write.mode("overwrite").json(str(quarantine_root))

        curated_files = upload_tree(client, bucket, curated_root, "curated/commerce_events")
        quarantined_files = upload_tree(client, bucket, quarantine_root, "quarantine/curation") if invalid_count else 0

        by_type = {r["event_type"]: r["count"] for r in valid.groupBy("event_type").count().collect()}
        report = {
            "raw_objects": downloaded,
            "valid_rows": valid_count,
            "invalid_rows": invalid_count,
            "duplicate_rows_removed": duplicate_count,
            "curated_files": curated_files,
            "quarantine_files": quarantined_files,
            "rows_by_event_type": by_type,
        }
        body = json.dumps(report, indent=2, sort_keys=True).encode()
        client.put_object(Bucket=bucket, Key="quality/curation/latest.json", Body=body, ContentType="application/json")
        print(json.dumps(report, indent=2, sort_keys=True))
        spark.stop()
    finally:
        shutil.rmtree(work, ignore_errors=True)

if __name__ == "__main__":
    main()
