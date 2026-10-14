# Architecture Notes

## Design goals

The platform separates the transactional write path from the analytical ingestion path. The source service can remain optimized for order processing, while downstream data consumers build independent projections without coupling back to the order database.

### Event contract

Every event uses a canonical envelope:

```json
{
  "eventId": "UUID",
  "eventType": "ORDER_CREATED",
  "entityType": "order",
  "entityId": "order-123",
  "occurredAt": "2026-10-14T12:00:00Z",
  "schemaVersion": 1,
  "source": "order-service",
  "payload": {}
}
```

Kafka keys use `entityId`, so events for the same aggregate are ordered within a partition. Topic names follow `commerce.<entityType>.v1`.

## Delivery semantics

Kafka consumers are configured for manual acknowledgement. An event is acknowledged only after the PostgreSQL ledger insert and S3 raw write complete. Duplicate delivery is safe because the ledger has a primary key on `event_id`, and the S3 object key is deterministic from that same ID. This creates effectively-once materialization for the demo's two sinks even though Kafka itself provides at-least-once delivery.

The order write path uses a **transactional outbox**: the order and its `ORDER_CREATED` event are committed in one PostgreSQL transaction. A scheduled relay locks unpublished outbox rows with `FOR UPDATE SKIP LOCKED`, publishes to Kafka, then marks them delivered. A crash after Kafka publish but before the delivered marker can produce a duplicate; downstream materialization is therefore intentionally idempotent. CDC/Debezium is the natural next step if polling the outbox is undesirable.

## Lake zones

- `raw/`: immutable JSON, partitioned by `entity_type` and event date.
- `curated/commerce_events/`: normalized, deduplicated Parquet.
- `quarantine/`: malformed or contract-invalid raw records.
- `quality/`: data-quality and reconciliation reports.

## Data-quality controls

The curation job checks required envelope fields, parses event timestamps, rejects unknown entity types, deduplicates `event_id`, and reports row counts by event type. Reconciliation independently compares source orders with downstream ingestion.

## Scaling model

Kafka partition count controls stream parallelism. Consumers can scale horizontally under a shared consumer group. S3 object paths avoid hot prefixes by distributing data across date and entity partitions. Curated Parquet is compacted by Spark rather than exposing one analytical file per source event.
