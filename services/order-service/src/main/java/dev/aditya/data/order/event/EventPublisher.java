package dev.aditya.data.order.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.aditya.data.order.model.CommerceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class EventPublisher {
    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final TopicResolver topicResolver;

    public EventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper, TopicResolver topicResolver) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topicResolver = topicResolver;
    }

    public void publish(CommerceEvent event) {
        try {
            publishRawAndWait(topicResolver.topicFor(event.entityType()), event.entityId(), objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to serialize commerce event", e);
        }
    }

    public void publishRawAndWait(String topic, String key, String payload) {
        try {
            var result = kafkaTemplate.send(topic, key, payload).get(30, TimeUnit.SECONDS);
            log.info("Published topic={} key={} partition={} offset={}", topic, key,
                    result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
        } catch (Exception e) {
            throw new IllegalStateException("Kafka publish failed for topic=" + topic + " key=" + key, e);
        }
    }
}
