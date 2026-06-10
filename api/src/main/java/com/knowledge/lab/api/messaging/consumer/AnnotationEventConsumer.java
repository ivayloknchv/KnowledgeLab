package com.knowledge.lab.api.messaging.consumer;

import com.knowledge.lab.api.messaging.event.AnnotationEvent;
import com.knowledge.lab.api.messaging.handler.AnnotationEventHandler;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Single Kafka consumer for the annotation-events topic.
 *
 * Design:
 *  - Uses MANUAL_IMMEDIATE acknowledgment — message is committed only
 *    after ALL handlers succeed, preventing silent data loss.
 *  - Dispatches to a handler registry built from every {@link AnnotationEventHandler}
 *    bean in the application context.  Adding a new handler = zero changes here.
 *  - Each handler is invoked inside its own try/catch so one failing handler
 *    (e.g. analytics) cannot block a critical handler (e.g. audit log).
 *  - Dead-letter topic is configured in {@link com.knowledge.lab.api.config.KafkaConfig}
 *    so unrecoverable messages don't block the partition.
 */
@Slf4j
@Component
public class AnnotationEventConsumer {

    private final List<AnnotationEventHandler<?>> handlers;

    /**
     * Maps event class → list of handlers; built once at startup.
     * Using a raw-type map is intentional — type safety is enforced
     * at registration time via {@code supports()}.
     */
    @SuppressWarnings("rawtypes")
    private final Map<Class<?>, List<AnnotationEventHandler>> dispatchMap = new HashMap<>();

    public AnnotationEventConsumer(List<AnnotationEventHandler<?>> handlers) {
        this.handlers = handlers;
    }

    @PostConstruct
    @SuppressWarnings(value = "rawtypes")
    void buildDispatchMap() {
        for (AnnotationEventHandler<?> handler : handlers) {
            dispatchMap
                    .computeIfAbsent(handler.supports(), k -> new java.util.ArrayList<>())
                    .add(handler);
        }
        log.info("AnnotationEventConsumer registered {} handler(s) across {} event type(s).",
                handlers.size(), dispatchMap.size());
    }

    @KafkaListener(
            topics              = "#{kafkaProperties.topics.annotationEvents}",
            groupId             = "#{kafkaProperties.consumer.groupId}",
            containerFactory    = "annotationEventListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, AnnotationEvent> record, Acknowledgment ack) {
        AnnotationEvent event = record.value();

        if (event == null) {
            log.warn("Received null event from partition={} offset={} — skipping.",
                    record.partition(), record.offset());
            ack.acknowledge();
            return;
        }

        log.debug("Received {} [eventId={}] from partition={} offset={}",
                event.getClass().getSimpleName(), event.getEventId(),
                record.partition(), record.offset());

        dispatch(event);
        ack.acknowledge();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void dispatch(AnnotationEvent event) {
        List<AnnotationEventHandler> matched = dispatchMap.get(event.getClass());

        if (matched == null || matched.isEmpty()) {
            log.debug("No handler registered for event type: {}", event.getClass().getSimpleName());
            return;
        }

        for (AnnotationEventHandler handler : matched) {
            try {
                handler.handle(event);
            } catch (Exception ex) {
                // Log and continue — one handler failure must not silence others.
                log.error(
                        "Handler {} failed for event {} [eventId={}]: {}",
                        handler.getClass().getSimpleName(),
                        event.getClass().getSimpleName(),
                        event.getEventId(),
                        ex.getMessage(), ex
                );
            }
        }
    }
}
