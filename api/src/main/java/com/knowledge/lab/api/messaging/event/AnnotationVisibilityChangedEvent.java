package com.knowledge.lab.api.messaging.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Raised when an annotation's visibility is toggled between private and public.
 * Useful for notification consumers: "X shared their highlight with the group."
 */
@Getter
@SuperBuilder
@NoArgsConstructor
public final class AnnotationVisibilityChangedEvent extends AnnotationEvent {

    private boolean wasPublic;

    @JsonProperty("isNowPublic")
    private boolean isNowPublic;
}
