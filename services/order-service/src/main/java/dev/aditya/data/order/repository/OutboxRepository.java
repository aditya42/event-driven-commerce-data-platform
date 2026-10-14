package dev.aditya.data.order.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class OutboxRepository {
    public record OutboxRow(UUID eventId, String aggregateId, String topic, String eventKey, String payload, int attempts) {}

    private final JdbcTemplate jdbc;

    public OutboxRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void add(UUID eventId, String aggregateId, String topic, String eventKey, String payload) {
        jdbc.update("INSERT INTO outbox_events(event_id,aggregate_id,topic,event_key,payload) VALUES (?,?,?,?,cast(? as jsonb))",
                eventId, aggregateId, topic, eventKey, payload);
    }

    public List<OutboxRow> lockBatch(int limit) {
        return jdbc.query("""
                SELECT event_id, aggregate_id, topic, event_key, payload::text, attempts
                FROM outbox_events
                WHERE published_at IS NULL
                ORDER BY created_at
                FOR UPDATE SKIP LOCKED
                LIMIT ?
                """, (rs, rowNum) -> new OutboxRow(
                rs.getObject("event_id", UUID.class), rs.getString("aggregate_id"), rs.getString("topic"),
                rs.getString("event_key"), rs.getString("payload"), rs.getInt("attempts")), limit);
    }

    public void markPublished(UUID eventId) {
        jdbc.update("UPDATE outbox_events SET published_at=now(), attempts=attempts+1, last_error=NULL WHERE event_id=?", eventId);
    }

    public void markFailed(UUID eventId, String error) {
        jdbc.update("UPDATE outbox_events SET attempts=attempts+1, last_error=? WHERE event_id=?", truncate(error), eventId);
    }

    private static String truncate(String value) {
        if (value == null) return null;
        return value.length() <= 2000 ? value : value.substring(0, 2000);
    }
}
