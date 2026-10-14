package dev.aditya.data.ingestion.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.aditya.data.ingestion.model.CommerceEvent;
import dev.aditya.data.ingestion.store.EventLedgerRepository;
import dev.aditya.data.ingestion.store.RawLakeWriter;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class CommerceEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(CommerceEventConsumer.class);
    private final ObjectMapper mapper;
    private final EventLedgerRepository ledger;
    private final RawLakeWriter lakeWriter;

    public CommerceEventConsumer(ObjectMapper mapper, EventLedgerRepository ledger, RawLakeWriter lakeWriter) {
        this.mapper = mapper;
        this.ledger = ledger;
        this.lakeWriter = lakeWriter;
    }

    @KafkaListener(topicPattern = "commerce\\.(customer|product|inventory|cart|order|payment|shipment|refund)\\.v1")
    public void consume(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) throws Exception {
        CommerceEvent event = mapper.readValue(record.value(), CommerceEvent.class);
        validate(event);
        String s3Key = lakeWriter.keyFor(event);
        boolean inserted = ledger.insertIfAbsent(event, record.topic(), record.partition(), record.offset(), s3Key);
        lakeWriter.write(s3Key, record.value());
        acknowledgment.acknowledge();
        log.info("Ingested eventId={} duplicate={} topic={} partition={} offset={} s3Key={}",
                event.eventId(), !inserted, record.topic(), record.partition(), record.offset(), s3Key);
    }

    private static void validate(CommerceEvent event) {
        if (event.eventId() == null || event.eventType() == null || event.eventType().isBlank()
                || event.entityType() == null || event.entityType().isBlank()
                || event.entityId() == null || event.entityId().isBlank()
                || event.occurredAt() == null || event.schemaVersion() < 1
                || event.source() == null || event.source().isBlank() || event.payload() == null) {
            throw new IllegalArgumentException("Invalid commerce event envelope");
        }
    }
}
