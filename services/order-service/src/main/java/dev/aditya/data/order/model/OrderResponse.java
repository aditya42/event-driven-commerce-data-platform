package dev.aditya.data.order.model;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(
        String orderId,
        String customerId,
        String currency,
        BigDecimal totalAmount,
        String status,
        Instant createdAt) {
}
