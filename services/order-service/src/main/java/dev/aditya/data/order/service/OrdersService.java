package dev.aditya.data.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.aditya.data.order.event.EventPublisher;
import dev.aditya.data.order.event.TopicResolver;
import dev.aditya.data.order.model.*;
import dev.aditya.data.order.repository.OrderRepository;
import dev.aditya.data.order.repository.OutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrdersService {
    private final OrderRepository repository;
    private final OutboxRepository outbox;
    private final EventPublisher publisher;
    private final TopicResolver topicResolver;
    private final ObjectMapper mapper;

    public OrdersService(OrderRepository repository, OutboxRepository outbox, EventPublisher publisher,
                         TopicResolver topicResolver, ObjectMapper mapper) {
        this.repository = repository;
        this.outbox = outbox;
        this.publisher = publisher;
        this.topicResolver = topicResolver;
        this.mapper = mapper;
    }

    @Transactional
    public OrderResponse create(OrderRequest request) {
        String orderId = "ord-" + UUID.randomUUID();
        Instant now = Instant.now();
        BigDecimal total = request.items().stream()
                .map(i -> i.unitPrice().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        repository.insert(orderId, request.customerId(), request.currency(), total, now, request.items());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("customerId", request.customerId());
        payload.put("currency", request.currency());
        payload.put("totalAmount", total);
        payload.put("items", request.items());

        CommerceEvent event = new CommerceEvent(UUID.randomUUID(), "ORDER_CREATED", "order", orderId, now, 1, "order-service", payload);
        try {
            outbox.add(event.eventId(), orderId, topicResolver.topicFor("order"), orderId, mapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to serialize order event", e);
        }
        return new OrderResponse(orderId, request.customerId(), request.currency(), total, "CREATED", now);
    }

    public List<OrderResponse> list() { return repository.findAll(); }

    public CommerceEvent emit(DomainEventRequest request) {
        CommerceEvent event = new CommerceEvent(UUID.randomUUID(), request.eventType().toUpperCase(),
                request.entityType().toLowerCase(), request.entityId(), Instant.now(), 1, request.source(), request.payload());
        publisher.publish(event);
        return event;
    }
}
