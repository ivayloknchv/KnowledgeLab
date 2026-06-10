package com.knowledge.lab.api.messaging.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Raised when the body or colour of an annotation is edited.
 */
@Getter
@SuperBuilder
@NoArgsConstructor
public final class AnnotationUpdatedEvent extends AnnotationEvent {

    private String  newBody;
    private String  newColor;
}
