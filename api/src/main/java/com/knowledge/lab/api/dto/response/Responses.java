package com.knowledge.lab.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.knowledge.lab.api.model.*;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;

public final class Responses {

    private Responses() {}

    public record ContentResponse(
            String id,
            String title,
            String description,
            String author,
            Content.ContentType type,
            String shelfId,
            String shelfName,
            List<String> tags,
            String isbn,
            String doi,
            Integer year,
            String uploaderId,
            String uploaderName,
            String fileKey,
            String mimeType,
            Long fileSizeBytes,
            String coverImageUrl,
            long viewCount,
            long downloadCount,
            Content.ContentStatus status,
            Instant createdAt,
            Instant updatedAt,
            Instant publishedAt
    ) {
        public static ContentResponse from(Content c) {
            return new ContentResponse(
                    c.getId(), c.getTitle(), c.getDescription(), c.getAuthor(),
                    c.getType(), c.getShelfId(), null, c.getTags(),
                    c.getIsbn(), c.getDoi(), c.getYear(),
                    c.getUploaderId(), c.getUploaderName(),
                    c.getFileKey(), c.getMimeType(), c.getFileSizeBytes(), c.getCoverImageUrl(),
                    c.getViewCount(), c.getDownloadCount(), c.getStatus(),
                    c.getCreatedAt(), c.getUpdatedAt(), c.getPublishedAt()
            );
        }
    }

    public record SearchResponse(
            List<ContentResponse> items,
            long totalHits,
            int page,
            int size,
            int totalPages
    ) {}

    public record ShelfResponse(
            String id,
            String name,
            String description,
            String iconEmoji,
            String ownerId,
            // @JsonProperty pins the JSON key to "isPublic".
            // Without it, Jackson 3 strips the "is" prefix from the boolean accessor
            // isPublic() → serializes as "public" (a reserved word), which causes an
            // InvalidDefinitionException and a 500 on every shelf endpoint response.
            @JsonProperty("isPublic") boolean isPublic,
            long contentCount,
            Instant createdAt
    ) {
        public static ShelfResponse from(Shelf s) {
            return new ShelfResponse(
                    s.getId(), s.getName(), s.getDescription(), s.getIconEmoji(),
                    s.getOwnerId(), s.isPublic(), s.getContentCount(), s.getCreatedAt()
            );
        }
    }

    public record ReadingListResponse(
            String id,
            String ownerId,
            String name,
            String description,
            @JsonProperty("isPublic") boolean isPublic,
            int entryCount,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static ReadingListResponse from(ReadingList rl) {
            return new ReadingListResponse(
                    rl.getId(), rl.getOwnerId(), rl.getName(), rl.getDescription(),
                    rl.isPublic(), rl.getEntries().size(), rl.getCreatedAt(), rl.getUpdatedAt()
            );
        }
    }

    public record ReadingListDetailResponse(
            String id,
            String ownerId,
            String name,
            String description,
            @JsonProperty("isPublic") boolean isPublic,
            List<ReadingList.BookmarkEntry> entries,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static ReadingListDetailResponse from(ReadingList rl) {
            return new ReadingListDetailResponse(
                    rl.getId(), rl.getOwnerId(), rl.getName(), rl.getDescription(),
                    rl.isPublic(), rl.getEntries(), rl.getCreatedAt(), rl.getUpdatedAt()
            );
        }
    }

    public record AnnotationResponse(
            String id,
            String contentId,
            Integer pageNumber,
            Annotation.TextRange range,
            String selectedText,
            String authorId,
            String authorName,
            Annotation.AnnotationType type,
            String color,
            String body,
            @JsonProperty("isPublic") boolean isPublic,
            List<Annotation.Reply> replies,
            long replyCount,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static AnnotationResponse from(Annotation a) {
            return new AnnotationResponse(
                    a.getId(), a.getContentId(), a.getPageNumber(), a.getRange(),
                    a.getSelectedText(), a.getAuthorId(), a.getAuthorName(),
                    a.getType(), a.getColor(), a.getBody(), a.isPublic(),
                    a.getReplies(), a.getReplies().size(),
                    a.getCreatedAt(), a.getUpdatedAt()
            );
        }
    }

    public record PageResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean last
    ) {
        public static <T> PageResponse<T> from(Page<T> page) {
            return new PageResponse<>(
                    page.getContent(), page.getNumber(), page.getSize(),
                    page.getTotalElements(), page.getTotalPages(), page.isLast()
            );
        }
    }
}
