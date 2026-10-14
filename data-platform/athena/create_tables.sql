CREATE DATABASE IF NOT EXISTS commerce_analytics;

CREATE EXTERNAL TABLE IF NOT EXISTS commerce_analytics.commerce_events (
  event_id string,
  event_type string,
  entity_id string,
  occurred_at timestamp,
  schema_version int,
  source string,
  payload_json string
)
PARTITIONED BY (entity_type string, event_date date)
STORED AS PARQUET
LOCATION 's3://commerce-data-lake/curated/commerce_events/';

MSCK REPAIR TABLE commerce_analytics.commerce_events;

-- Example analytics query
SELECT event_date, event_type, count(*) AS event_count
FROM commerce_analytics.commerce_events
GROUP BY 1, 2
ORDER BY 1 DESC, 3 DESC;
