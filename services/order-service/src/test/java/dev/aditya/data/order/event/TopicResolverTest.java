package dev.aditya.data.order.event;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TopicResolverTest {
    private final TopicResolver resolver = new TopicResolver();

    @Test void resolvesKnownDomain() {
        assertEquals("commerce.payment.v1", resolver.topicFor("Payment"));
    }

    @Test void rejectsUnknownDomain() {
        assertThrows(IllegalArgumentException.class, () -> resolver.topicFor("unknown"));
    }
}
