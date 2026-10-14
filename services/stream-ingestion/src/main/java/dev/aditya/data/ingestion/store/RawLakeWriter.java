package dev.aditya.data.ingestion.store;

import dev.aditya.data.ingestion.model.CommerceEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RawLakeWriter {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);
    private final S3Client s3;
    private final String bucket;

    public RawLakeWriter(S3Client s3, @Value("${app.s3.bucket}") String bucket) {
        this.s3 = s3;
        this.bucket = bucket;
    }

    public String keyFor(CommerceEvent event) {
        return "raw/entity_type=" + event.entityType() + "/event_date=" + DATE.format(event.occurredAt()) + "/" + event.eventId() + ".json";
    }

    public void write(String key, String json) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket).key(key).contentType("application/json").build();
        s3.putObject(request, RequestBody.fromBytes(json.getBytes(StandardCharsets.UTF_8)));
    }
}
