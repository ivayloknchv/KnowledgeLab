package com.knowledge.lab.api.messaging.publisher;

import com.knowledge.lab.api.config.KafkaProperties;
import com.knowledge.lab.api.messaging.event.AnnotationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka adapter that fulfils the {@link AnnotationEventPublisher} port.
 *
 * Responsibilities:
 *  - Route every event to the correct Kafka topic (from {@link KafkaProperties}).
 *  - Use {@code annotationId} as the message key so all events for the same
 *    annotation land on the same partition, preserving ordering guarantees.
 *  - Log delivery confirmation and failures without rethrowing — the business
 *    operation already succeeded; Kafka failure must not roll it back.
 *
 * Why @Async:
 *  KafkaTemplate.send() is non-blocking AFTER the producer has topic metadata,
 *  but the very first send (or after a metadata refresh) blocks the calling thread
 *  for up to max.block.ms (default 60 s) while it fetches partition leaders from
 *  the broker. Without @Async, that stall holds the HTTP request thread, causing
 *  Postman / clients to see the request hang indefinitely even though the DB write
 *  already succeeded.  Running publish() on a task-executor thread decouples the
 *  HTTP response from broker latency entirely.
 *
 * This class is the ONLY place in the codebase that imports KafkaTemplate.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaAnnotationEventPublisher implements AnnotationEventPublisher {

    private final KafkaTemplate<String, AnnotationEvent> kafkaTemplate;
    private final KafkaProperties                        kafkaProperties;

    @Async   // ← runs on Spring's task executor; HTTP thread returns immediately
    @Override
    public void publish(AnnotationEvent event) {
        String topic = kafkaProperties.getTopics().getAnnotationEvents();
        String key   = event.getAnnotationId();   // partition by annotationId → ordered per annotation

        CompletableFuture<SendResult<String, AnnotationEvent>> future =
                kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error(
                        "Failed to publish {} [eventId={}] to topic '{}': {}",
                        event.getClass().getSimpleName(), event.getEventId(), topic, ex.getMessage(), ex
                );
            } else {
                log.debug(
                        "Published {} [eventId={}] → topic='{}' partition={} offset={}",
                        event.getClass().getSimpleName(),
                        event.getEventId(),
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset()
                );
            }
        });
    }
}
