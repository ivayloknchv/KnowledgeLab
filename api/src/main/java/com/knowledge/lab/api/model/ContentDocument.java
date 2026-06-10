package com.knowledge.lab.api.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.Instant;
import java.util.List;

/**
 * Elasticsearch mirror of a Content document.
 *
 * Kept lightweight — only the fields needed for search and filtering.
 * Synced from MongoDB via ContentEventListener whenever a Content is saved/deleted.
 */
@Document(indexName = "contents")
@Setting(settingPath = "elasticsearch/content-settings.json")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentDocument {

    @Id
    private String id;

    @MultiField(
            mainField  = @Field(type = FieldType.Text,    analyzer = "english"),
            otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword)
    )
    private String title;

    @Field(type = FieldType.Text, analyzer = "english")
    private String description;

    @Field(type = FieldType.Text, analyzer = "english")
    private String author;

    @Field(type = FieldType.Text, analyzer = "english")
    private List<String> tags;

    @Field(type = FieldType.Keyword)
    private String type;            // BOOK | ARTICLE | RESEARCH

    @Field(type = FieldType.Keyword)
    private String shelfId;

    @Field(type = FieldType.Keyword)
    private String shelfName;

    @Field(type = FieldType.Keyword)
    private String status;          // PUBLISHED etc.

    @Field(type = FieldType.Keyword)
    private String uploaderId;

    @Field(type = FieldType.Keyword)
    private String uploaderName;

    @Field(type = FieldType.Integer)
    private Integer year;

    @Field(type = FieldType.Long)
    private long viewCount;

    @Field(type = FieldType.Long)
    private long downloadCount;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private Instant publishedAt;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private Instant createdAt;

    private String coverImageUrl;
    private String isbn;
    private String doi;
}
