package dev.aditya.data.order.event;

import dev.aditya.data.order.repository.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxRelay {
    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private final OutboxRepository outbox;
    private final EventPublisher publisher;

    public OutboxRelay(OutboxRepository outbox, EventPublisher publisher) {
        this.outbox = outbox;
        this.publisher = publisher;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-ms:1000}")
    @Transactional
    public void relay() {
        for (OutboxRepository.OutboxRow row : outbox.lockBatch(50)) {
            try {
                publisher.publishRawAndWait(row.topic(), row.eventKey(), row.payload());
                outbox.markPublished(row.eventId());
            } catch (Exception e) {
                outbox.markFailed(row.eventId(), e.getMessage());
                log.warn("Outbox publish failed eventId={} attempts={}", row.eventId(), row.attempts() + 1, e);
            }
        }
    }
}
