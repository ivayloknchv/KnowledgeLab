package com.knowledge.lab.api.messaging.handler;

import com.knowledge.lab.api.messaging.event.AnnotationEvent;

/**
 * Strategy interface for handling a specific annotation domain event.
 *
 * Each handler is a focused, single-responsibility component.
 * New behavior (e.g. push notifications, email digests, ML signals)
 * is added by implementing this interface and registering the bean —
 * no existing code needs to change (Open/Closed Principle).
 *
 * @param <E> the concrete event type this handler processes
 */
public interface AnnotationEventHandler<E extends AnnotationEvent> {

    /**
     * The exact event class this handler can process.
     * Used by the consumer dispatcher for type-safe routing.
     */
    Class<E> supports();

    /**
     * Handle the event. Implementations must be idempotent — Kafka may
     * redeliver the same event on consumer restart.
     *
     * @param event the event to process; never {@code null}
     */
    void handle(E event);
}
