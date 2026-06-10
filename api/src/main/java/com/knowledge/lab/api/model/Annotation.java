package com.knowledge.lab.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a highlight, sticky note, or inline comment on a Content page.
 *
 * A single Annotation record anchors to a text range; replies form a
 * lightweight discussion thread stored as embedded documents.
 */
@Document(collection = "annotations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndexes({
        @CompoundIndex(name = "content_page", def = "{'contentId': 1, 'pageNumber': 1}")
})
public class Annotation {

    @Id
    private String id;

    @Indexed
    @NotBlank
    private String contentId;

    @NotNull
    private Integer pageNumber;         // 0-based page index

    private TextRange range;            // character offsets within the page text

    @NotBlank
    private String selectedText;        // snapshot of highlighted text

    @Indexed
    @NotBlank
    private String authorId;

    private String authorName;          // denormalized for display

    private AnnotationType type;        // HIGHLIGHT | NOTE | QUESTION

    private String color;               // hex colour for highlights: "#FFD700"

    private String body;                // text of the note (may be null for plain highlights)

    @JsonProperty("isPublic")
    @Builder.Default
    private boolean isPublic = false;   // private by default; share = true makes it collaborative

    @Builder.Default
    private List<Reply> replies = new ArrayList<>();

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    public enum AnnotationType {
        HIGHLIGHT, NOTE, QUESTION
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TextRange {
        private int start;
        private int end;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Reply {
        private String id;
        private String authorId;
        private String authorName;
        private String body;
        private Instant createdAt;
        private Instant updatedAt;
    }
}
