package dev.aditya.data.ingestion.store;

import dev.aditya.data.ingestion.model.CommerceEvent;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class RawLakeWriterTest {
    @Test void generatesDeterministicPartitionedKey() {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000123");
        CommerceEvent event = new CommerceEvent(id, "ORDER_CREATED", "order", "ord-1",
                Instant.parse("2026-10-14T12:30:00Z"), 1, "test", Map.of());
        RawLakeWriter writer = new RawLakeWriter(mock(S3Client.class), "bucket");
        assertEquals("raw/entity_type=order/event_date=2026-10-14/00000000-0000-0000-0000-000000000123.json", writer.keyFor(event));
    }
}
