package com.knowledge.lab.api.messaging.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Raised when a reply is removed from a discussion thread.
 */
@Getter
@SuperBuilder
@NoArgsConstructor
public final class ReplyDeletedEvent extends AnnotationEvent {

    private String  replyId;
    private boolean deletedByAdmin;
}
