package com.knowledge.lab.api.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Core content entity — represents any uploadable material:
 * book, article, research paper, etc.
 */
@Document(collection = "contents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Content {

    @Id
    private String id;

    @Indexed
    @NotBlank
    private String uploaderId;      // User.id

    @Indexed
    private String uploaderName;    // denormalised for display

    @NotNull
    private ContentType type;       // BOOK | ARTICLE | RESEARCH

    @Indexed
    private String shelfId;         // Shelf.id — the "category"

    @NotBlank
    @TextIndexed(weight = 10)
    private String title;

    @TextIndexed(weight = 5)
    private String description;

    @TextIndexed(weight = 3)
    private String author;          // original author(s)

    @Builder.Default
    private List<String> tags = new ArrayList<>();

    private String isbn;            // optional for books
    private String doi;             // optional for research papers
    private Integer year;           // publication year

    private String fileKey;         // S3 / MinIO object key (or local path)
    private String mimeType;
    private Long   fileSizeBytes;
    private String coverImageUrl;

    @Builder.Default
    private long viewCount = 0;

    @Builder.Default
    private long downloadCount = 0;

    @Builder.Default
    private ContentStatus status = ContentStatus.DRAFT;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    private Instant publishedAt;

    public enum ContentType {
        BOOK, ARTICLE, RESEARCH
    }

    public enum ContentStatus {
        DRAFT, PUBLISHED, ARCHIVED
    }
}
