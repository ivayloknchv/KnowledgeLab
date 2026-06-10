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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A named, ordered list of Content items that a user curates.
 * One user may have many lists (e.g. "To Read", "Favourites", "Quantum Physics").
 */
@Document(collection = "reading_lists")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndexes({
        @CompoundIndex(name = "owner_name", def = "{'ownerId': 1, 'name': 1}", unique = true)
})
public class ReadingList {

    @Id
    private String id;

    @Indexed
    private String ownerId;

    private String name;
    private String description;

    @JsonProperty("isPublic")
    @Builder.Default
    private boolean isPublic = false;

    @Builder.Default
    private List<BookmarkEntry> entries = new ArrayList<>();

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookmarkEntry {
        private String contentId;
        private String note;            // optional personal note
        private boolean read;
        private Instant addedAt;
        private Instant readAt;
    }
}
