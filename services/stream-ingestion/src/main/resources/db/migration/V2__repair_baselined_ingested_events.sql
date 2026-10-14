-- Recover ingested_events if another service populated public before ingestion Flyway started.
-- Re-entrant on databases where V1 already ran.
CREATE TABLE IF NOT EXISTS ingested_events (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(80) NOT NULL,
    entity_type VARCHAR(40) NOT NULL,
    entity_id VARCHAR(128) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    schema_version INTEGER NOT NULL,
    source VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    kafka_topic VARCHAR(120) NOT NULL,
    kafka_partition INTEGER NOT NULL,
    kafka_offset BIGINT NOT NULL,
    s3_key TEXT NOT NULL,
    ingested_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_ingested_events_entity ON ingested_events(entity_type, entity_id, occurred_at);
CREATE INDEX IF NOT EXISTS idx_ingested_events_type_time ON ingested_events(event_type, occurred_at);
