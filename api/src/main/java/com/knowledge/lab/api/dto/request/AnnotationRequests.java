package com.knowledge.lab.api.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.knowledge.lab.api.model.Annotation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public sealed interface AnnotationRequests {

    record CreateAnnotationRequest(
            @NotBlank  String contentId,
            @NotNull   Integer pageNumber,
            @NotNull   Annotation.TextRange range,
            @NotBlank  String selectedText,
            @NotNull   Annotation.AnnotationType type,
            String color,
            String body,
            @JsonProperty("isPublic") boolean isPublic
    ) implements AnnotationRequests {}

    record UpdateAnnotationRequest(
            String body,
            String color,
            @JsonProperty("isPublic") Boolean isPublic
    ) implements AnnotationRequests {}

    record AddReplyRequest(
            @NotBlank String body
    ) implements AnnotationRequests {}
}
