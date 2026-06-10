package com.knowledge.lab.api.service;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import com.knowledge.lab.api.dto.request.ContentRequests;
import com.knowledge.lab.api.dto.response.Responses;
import com.knowledge.lab.api.model.Content;
import com.knowledge.lab.api.model.ContentDocument;
import com.knowledge.lab.api.repository.ContentSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Elasticsearch-powered search and discovery service.
 *
 * Supports:
 *  - Full-text multi-field search with boosting
 *  - Keyword filters (type, shelfId, uploaderId, year range, tags)
 *  - Sorting by relevance, date, views, downloads
 *  - Highlighting matched terms
 *  - Suggestions (prefix-based autocomplete on title.keyword)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentSearchService {

    private final ElasticsearchOperations  esOps;
    private final ContentSearchRepository  searchRepository;

    public void index(Content content) {
        ContentDocument doc = toDocument(content);
        searchRepository.save(doc);
        log.debug("Indexed content {}", content.getId());
    }

    public void delete(String contentId) {
        searchRepository.deleteById(contentId);
        log.debug("Removed content {} from index", contentId);
    }

    public Responses.SearchResponse search(ContentRequests.ContentSearchRequest req) {
        NativeQuery query = buildQuery(req);

        SearchHits<ContentDocument> hits = esOps.search(query, ContentDocument.class);

        List<Responses.ContentResponse> items = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(this::toContentResponse)
                .toList();

        long totalHits = hits.getTotalHits();
        int  totalPages = (int) Math.ceil((double) totalHits / req.size());

        return new Responses.SearchResponse(items, totalHits, req.page(), req.size(), totalPages);
    }

    /**
     * Lightweight autocomplete — returns title suggestions matching a prefix.
     */
    public List<String> suggest(String prefix, int maxSuggestions) {
        var query = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> b
                        .must(m -> m.prefix(p -> p
                                .field("title.keyword")
                                .value(prefix)))
                        .filter(f -> f.term(t -> t
                                .field("status")
                                .value("PUBLISHED")))
                ))
                .withPageable(PageRequest.of(0, maxSuggestions))
                .withSourceFilter(
                        new org.springframework.data.elasticsearch.core.query.FetchSourceFilter(
                        true, new String[]{"title"}, null))
                .build();

        return esOps.search(query, ContentDocument.class)
                .getSearchHits()
                .stream()
                .map(h -> h.getContent().getTitle())
                .distinct()
                .toList();
    }

    private NativeQuery buildQuery(ContentRequests.ContentSearchRequest req) {
        List<Query> filters = new ArrayList<>();

        // Always restrict to published content in public search
        filters.add(Query.of(q -> q.term(t -> t.field("status").value("PUBLISHED"))));

        if (req.type() != null && !req.type().isBlank()) {
            filters.add(Query.of(q -> q.term(t -> t.field("type").value(req.type()))));
        }
        if (req.shelfId() != null && !req.shelfId().isBlank()) {
            filters.add(Query.of(q -> q.term(t -> t.field("shelfId").value(req.shelfId()))));
        }
        if (req.uploaderId() != null && !req.uploaderId().isBlank()) {
            filters.add(Query.of(q -> q.term(t -> t.field("uploaderId").value(req.uploaderId()))));
        }
        if (req.tags() != null && !req.tags().isEmpty()) {
            filters.add(Query.of(q -> q.terms(t -> t.field("tags")
                    .terms(tv -> tv.value(req.tags().stream()
                            .map(co.elastic.clients.elasticsearch._types.FieldValue::of)
                            .toList())))));
        }

        if (req.yearFrom() != null || req.yearTo() != null) {
            filters.add(Query.of(q -> q.range(r -> {
                r.number(n -> {
                    n.field("year");
                    if (req.yearFrom() != null) n.gte((double) req.yearFrom());
                    if (req.yearTo()   != null) n.lte((double) req.yearTo());
                    return n;
                });
                return r;
            })));
        }

        Query mainQuery;
        if (req.query() != null && !req.query().isBlank()) {
            mainQuery = Query.of(q -> q.multiMatch(m -> m
                    .query(req.query())
                    .fields(List.of("title^4", "author^2", "description^1.5", "tags^1"))
                    .type(TextQueryType.BestFields)
                    .fuzziness("AUTO")
                    .operator(Operator.Or)
            ));
        } else {
            mainQuery = Query.of(q -> q.matchAll(m -> m));
        }

        Query finalQuery = Query.of(q -> q.bool(b -> b
                .must(mainQuery)
                .filter(filters)
        ));

        var queryBuilder = NativeQuery.builder()
                .withQuery(finalQuery)
                .withPageable(PageRequest.of(req.page(), req.size()))
                .withHighlightQuery(buildHighlight());

        boolean desc = !"asc".equalsIgnoreCase(req.sortDir());
        SortOrder order = desc ? SortOrder.Desc : SortOrder.Asc;

        switch (req.sortBy()) {
            case "date"      -> queryBuilder.withSort(s -> s.field(f -> f.field("publishedAt").order(order)));
            case "views"     -> queryBuilder.withSort(s -> s.field(f -> f.field("viewCount").order(order)));
            case "downloads" -> queryBuilder.withSort(s -> s.field(f -> f.field("downloadCount").order(order)));
            // relevance = default ES scoring, no explicit sort needed
        }

        return queryBuilder.build();
    }

    private org.springframework.data.elasticsearch.core.query.HighlightQuery buildHighlight() {
        var highlight = new org.springframework.data.elasticsearch.core.query.highlight.Highlight(
                List.of(
                        new org.springframework.data.elasticsearch.core.query.highlight.HighlightField("title"),
                        new org.springframework.data.elasticsearch.core.query.highlight.HighlightField("description"),
                        new org.springframework.data.elasticsearch.core.query.highlight.HighlightField("author")
                )
        );
        return new org.springframework.data.elasticsearch.core.query.HighlightQuery(highlight, ContentDocument.class);
    }

    private ContentDocument toDocument(Content c) {
        return ContentDocument.builder()
                .id(c.getId())
                .title(c.getTitle())
                .description(c.getDescription())
                .author(c.getAuthor())
                .tags(c.getTags())
                .type(c.getType() != null ? c.getType().name() : null)
                .shelfId(c.getShelfId())
                .status(c.getStatus() != null ? c.getStatus().name() : null)
                .uploaderId(c.getUploaderId())
                .uploaderName(c.getUploaderName())
                .year(c.getYear())
                .viewCount(c.getViewCount())
                .downloadCount(c.getDownloadCount())
                .publishedAt(c.getPublishedAt())
                .createdAt(c.getCreatedAt())
                .coverImageUrl(c.getCoverImageUrl())
                .isbn(c.getIsbn())
                .doi(c.getDoi())
                .build();
    }

    private Responses.ContentResponse toContentResponse(ContentDocument doc) {
        return new Responses.ContentResponse(
                doc.getId(),
                doc.getTitle(),
                doc.getDescription(),
                doc.getAuthor(),
                doc.getType() != null ? Content.ContentType.valueOf(doc.getType()) : null,
                doc.getShelfId(),
                doc.getShelfName(),
                doc.getTags(),
                doc.getIsbn(),
                doc.getDoi(),
                doc.getYear(),
                doc.getUploaderId(),
                doc.getUploaderName(),
                null,
                null,
                null,
                doc.getCoverImageUrl(),
                doc.getViewCount(),
                doc.getDownloadCount(),
                doc.getStatus() != null ? Content.ContentStatus.valueOf(doc.getStatus()) : null,
                doc.getCreatedAt(),
                null,
                doc.getPublishedAt()
        );
    }
}
