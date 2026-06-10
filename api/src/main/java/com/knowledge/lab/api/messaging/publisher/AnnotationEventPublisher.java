package com.knowledge.lab.api.messaging.publisher;

import com.knowledge.lab.api.messaging.event.AnnotationEvent;

/**
 * Port (hexagonal architecture) for publishing annotation domain events.
 *
 * {@link com.knowledge.lab.api.service.AnnotationService} depends on this
 * interface only — it has zero knowledge of Kafka, queues, or any other
 * transport.  Swap the adapter (e.g. for tests) without touching business logic.
 */
public interface AnnotationEventPublisher {

    /**
     * Publish a single domain event asynchronously.
     *
     * @param event the event to publish; must not be {@code null}
     */
    void publish(AnnotationEvent event);

}
