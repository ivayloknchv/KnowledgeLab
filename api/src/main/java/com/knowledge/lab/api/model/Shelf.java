package com.knowledge.lab.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

/**
 * A Shelf acts as a category/folder for organizing Content.
 * Shelves can be public (shared library) or private (personal).
 */
@Document(collection = "shelves")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Shelf {

    @Id
    private String id;

    @NotBlank
    private String name;

    private String description;

    private String iconEmoji;

    /** null = system shelf (global), set = personal shelf */
    @Indexed
    private String ownerId;

    // @JsonProperty pins the serialized name to "isPublic", preventing Jackson 3
    // from stripping the "is" prefix (which would produce "public" — a reserved word —
    // and cause an InvalidDefinitionException / 500 when serializing the response).
    @JsonProperty("isPublic")
    @Builder.Default
    private boolean isPublic = true;

    @Builder.Default
    private long contentCount = 0;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
