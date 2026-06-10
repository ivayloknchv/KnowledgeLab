package com.knowledge.lab.api.dto.request;

import com.knowledge.lab.api.model.Content;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public sealed interface ContentRequests {

    record CreateContentRequest(
            @NotBlank String title,
            String description,
            @NotNull Content.ContentType type,
            String shelfId,
            String author,
            List<String> tags,
            String isbn,
            String doi,
            Integer year,
            String fileKey,
            String mimeType,
            Long fileSizeBytes,
            String coverImageUrl
    ) implements ContentRequests {}

    record UpdateContentRequest(
            String title,
            String description,
            String shelfId,
            String author,
            List<String> tags,
            String isbn,
            String doi,
            Integer year,
            String coverImageUrl
    ) implements ContentRequests {}

    record ContentSearchRequest(
            String query,
            String type,            // BOOK | ARTICLE | RESEARCH | null = all
            String shelfId,
            String uploaderId,
            Integer yearFrom,
            Integer yearTo,
            List<String> tags,
            String sortBy,          // relevance | date | views | downloads
            String sortDir,         // asc | desc
            int page,
            int size
    ) implements ContentRequests {
        public ContentSearchRequest {
            if (page < 0) page = 0;
            if (size < 1 || size > 50) size = 20;
            if (sortBy == null) sortBy = "relevance";
            if (sortDir == null) sortDir = "desc";
        }
    }
}
