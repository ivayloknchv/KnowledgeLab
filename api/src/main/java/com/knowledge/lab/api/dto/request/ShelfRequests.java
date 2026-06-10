package com.knowledge.lab.api.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public sealed interface ShelfRequests {

    record CreateShelfRequest(
            @NotBlank String name,
            String description,
            String iconEmoji,
            @JsonProperty("isPublic") boolean isPublic
    ) implements ShelfRequests {}

    record UpdateShelfRequest(
            String name,
            String description,
            String iconEmoji,
            @JsonProperty("isPublic") Boolean isPublic
    ) implements ShelfRequests {}
}
