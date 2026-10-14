package dev.aditya.data.order.event;

import org.springframework.stereotype.Component;
import java.util.Set;

@Component
public class TopicResolver {
    private static final Set<String> ALLOWED = Set.of(
            "customer", "product", "inventory", "cart", "order", "payment", "shipment", "refund");

    public String topicFor(String entityType) {
        if (entityType == null || !ALLOWED.contains(entityType.toLowerCase())) {
            throw new IllegalArgumentException("Unsupported entityType: " + entityType);
        }
        return "commerce." + entityType.toLowerCase() + ".v1";
    }
}
