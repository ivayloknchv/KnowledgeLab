package com.knowledge.lab.api.messaging.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Raised when an existing reply's body is edited.
 */
@Getter
@SuperBuilder
@NoArgsConstructor
public final class ReplyUpdatedEvent extends AnnotationEvent {

    private String replyId;
    private String newBody;
}
