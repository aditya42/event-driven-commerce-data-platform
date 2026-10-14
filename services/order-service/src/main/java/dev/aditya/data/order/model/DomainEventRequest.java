package dev.aditya.data.order.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record DomainEventRequest(
        @NotBlank String eventType,
        @NotBlank String entityType,
        @NotBlank String entityId,
        @NotBlank String source,
        @NotNull Map<String, Object> payload) {
}
