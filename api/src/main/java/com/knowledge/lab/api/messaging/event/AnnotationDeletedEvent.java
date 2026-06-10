package com.knowledge.lab.api.messaging.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Raised when an annotation is permanently deleted.
 * Consumers (e.g. audit log) should treat this as a terminal event.
 */
@Getter
@SuperBuilder
@NoArgsConstructor
public final class AnnotationDeletedEvent extends AnnotationEvent {

    /** True when deleted by an admin rather than the original author. */
    private boolean deletedByAdmin;
}
