package com.knowledge.lab.api.messaging.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for every annotation-related domain event.
 *
 * Design principles:
 *  - Plain Java record-style value object — zero framework coupling.
 *  - {@code eventId} enables idempotent consumer logic.
 *  - {@code occurredAt} is set at creation time, never mutated.
 *  - Polymorphic JSON via @JsonTypeInfo so consumers can deserialize
 *    the correct subtype from the Kafka payload without a schema registry.
 */
@Getter
@SuperBuilder
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AnnotationCreatedEvent.class,            name = "ANNOTATION_CREATED"),
        @JsonSubTypes.Type(value = AnnotationUpdatedEvent.class,            name = "ANNOTATION_UPDATED"),
        @JsonSubTypes.Type(value = AnnotationDeletedEvent.class,            name = "ANNOTATION_DELETED"),
        @JsonSubTypes.Type(value = AnnotationVisibilityChangedEvent.class,  name = "ANNOTATION_VISIBILITY_CHANGED"),
        @JsonSubTypes.Type(value = ReplyAddedEvent.class,                   name = "REPLY_ADDED"),
        @JsonSubTypes.Type(value = ReplyUpdatedEvent.class,                 name = "REPLY_UPDATED"),
        @JsonSubTypes.Type(value = ReplyDeletedEvent.class,                 name = "REPLY_DELETED"),
})
public abstract sealed class AnnotationEvent
        permits AnnotationCreatedEvent,
        AnnotationUpdatedEvent,
        AnnotationDeletedEvent,
        AnnotationVisibilityChangedEvent,
        ReplyAddedEvent,
        ReplyUpdatedEvent,
        ReplyDeletedEvent {

    /** Globally unique event identifier — used for idempotency checks. */
    private final String  eventId     = UUID.randomUUID().toString();

    /** Wall-clock timestamp of when the event was raised (immutable). */
    private final Instant occurredAt  = Instant.now();

    /** The annotation this event concerns. */
    private String annotationId;

    /** The document this annotation belongs to. */
    private String contentId;

    /** User who triggered the action. */
    private String actorId;

    /** Display name of the actor — denormalized for consumers. */
    private String actorName;
}
