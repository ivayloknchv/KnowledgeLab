package com.knowledge.lab.api.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public sealed interface ReadingListRequests {

    record CreateReadingListRequest(
            @NotBlank String name,
            String description,
            @JsonProperty("isPublic") boolean isPublic
    ) implements ReadingListRequests {}

    record AddBookmarkRequest(
            @NotBlank String contentId,
            String note
    ) implements ReadingListRequests {}

    record UpdateBookmarkRequest(
            String note,
            Boolean read
    ) implements ReadingListRequests {}
}
