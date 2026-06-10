package com.knowledge.lab.api.messaging.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.knowledge.lab.api.model.Annotation;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Raised when a new annotation (highlight / note / question) is created.
 */
@Getter
@SuperBuilder
@NoArgsConstructor
public final class AnnotationCreatedEvent extends AnnotationEvent {

    private Annotation.AnnotationType annotationType;
    private Integer                   pageNumber;
    private String                    selectedText;
    private String                    body;

    @JsonProperty("isPublic")
    private boolean                   isPublic;
}
