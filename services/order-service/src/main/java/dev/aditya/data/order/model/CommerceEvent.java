package dev.aditya.data.order.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record CommerceEvent(
        UUID eventId,
        String eventType,
        String entityType,
        String entityId,
        Instant occurredAt,
        int schemaVersion,
        String source,
        Map<String, Object> payload) {
}
