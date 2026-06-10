package com.knowledge.lab.api.controller;

import com.knowledge.lab.api.dto.request.ContentRequests;
import com.knowledge.lab.api.dto.response.Responses;
import com.knowledge.lab.api.service.ContentSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Search & Discovery endpoints
 *
 * POST /api/search          — advanced full-text search with filters
 * GET  /api/search/suggest  — autocomplete title suggestions
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final ContentSearchService contentSearchService;

    /**
     * Full-featured search endpoint.
     *
     * Example request body:
     * {
     *   "query":    "quantum computing",
     *   "type":     "RESEARCH",
     *   "shelfId":  "abc123",
     *   "yearFrom": 2020,
     *   "yearTo":   2024,
     *   "tags":     ["physics", "computing"],
     *   "sortBy":   "date",
     *   "sortDir":  "desc",
     *   "page":     0,
     *   "size":     20
     * }
     */
    @PostMapping
    public ResponseEntity<Responses.SearchResponse> search(
            @RequestBody ContentRequests.ContentSearchRequest req) {
        return ResponseEntity.ok(contentSearchService.search(req));
    }

    /**
     * Autocomplete suggestions for the search bar.
     *
     * GET /api/search/suggest?q=quant&limit=5
     */
    @GetMapping("/suggest")
    public ResponseEntity<List<String>> suggest(
            @RequestParam String q,
            @RequestParam(defaultValue = "8") int limit) {
        if (q.isBlank() || q.length() < 2) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(contentSearchService.suggest(q, Math.min(limit, 20)));
    }
}
