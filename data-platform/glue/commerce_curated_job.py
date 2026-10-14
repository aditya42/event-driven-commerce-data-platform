import sys
from awsglue.utils import getResolvedOptions
from awsglue.context import GlueContext
from pyspark.context import SparkContext
from pyspark.sql import functions as F

args = getResolvedOptions(sys.argv, ["JOB_NAME", "SOURCE_PATH", "TARGET_PATH"])
sc = SparkContext.getOrCreate()
glue = GlueContext(sc)
spark = glue.spark_session
spark.conf.set("spark.sql.session.timeZone", "UTC")

allowed = ["customer", "product", "inventory", "cart", "order", "payment", "shipment", "refund"]
df = spark.read.option("recursiveFileLookup", "true").json(args["SOURCE_PATH"])

curated = (df
    .select(
        F.col("eventId").cast("string").alias("event_id"),
        F.upper("eventType").alias("event_type"),
        F.lower("entityType").alias("entity_type"),
        F.col("entityId").cast("string").alias("entity_id"),
        F.to_timestamp("occurredAt").alias("occurred_at"),
        F.col("schemaVersion").cast("int").alias("schema_version"),
        F.col("source").cast("string").alias("source"),
        F.to_json("payload").alias("payload_json"))
    .filter(F.col("event_id").isNotNull() & F.col("entity_type").isin(allowed) & F.col("occurred_at").isNotNull())
    .dropDuplicates(["event_id"])
    .withColumn("event_date", F.to_date("occurred_at")))

(curated.write.mode("overwrite")
 .partitionBy("entity_type", "event_date")
 .parquet(args["TARGET_PATH"]))
