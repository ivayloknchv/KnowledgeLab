package com.knowledge.lab.api.messaging.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Raised when a new reply is added to an annotation's discussion thread.
 *
 * The annotation author is denormalized here so notification handlers
 * can target them without an extra database round-trip.
 */
@Getter
@SuperBuilder
@NoArgsConstructor
public final class ReplyAddedEvent extends AnnotationEvent {

    private String replyId;
    private String replyBody;

    /** ID of the annotation's original author — primary notification target. */
    private String annotationAuthorId;
}
