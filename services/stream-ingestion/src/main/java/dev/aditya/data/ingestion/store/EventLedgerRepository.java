package dev.aditya.data.ingestion.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.aditya.data.ingestion.model.CommerceEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

@Repository
public class EventLedgerRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public EventLedgerRepository(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public boolean insertIfAbsent(CommerceEvent event, String topic, int partition, long offset, String s3Key) {
        try {
            int rows = jdbc.update("""
                    INSERT INTO ingested_events(event_id,event_type,entity_type,entity_id,occurred_at,schema_version,source,payload,kafka_topic,kafka_partition,kafka_offset,s3_key)
                    VALUES (?,?,?,?,?,?,?,cast(? as jsonb),?,?,?,?)
                    ON CONFLICT (event_id) DO NOTHING
                    """, event.eventId(), event.eventType(), event.entityType(), event.entityId(), Timestamp.from(event.occurredAt()),
                    event.schemaVersion(), event.source(), mapper.writeValueAsString(event.payload()), topic, partition, offset, s3Key);
            return rows == 1;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to serialize payload", e);
        }
    }
}
