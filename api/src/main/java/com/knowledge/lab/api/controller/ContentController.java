package com.knowledge.lab.api.controller;

import com.knowledge.lab.api.dto.request.ContentRequests;
import com.knowledge.lab.api.dto.response.Responses;
import com.knowledge.lab.api.service.ContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Content Upload, Organization & Publishing
 *
 * POST     /api/contents                     — upload / create a new content item
 * GET      /api/contents/{id}                — get content by id (increments view count)
 * PATCH    /api/contents/{id}                — update metadata
 * DELETE   /api/contents/{id}                — delete (owner or admin)
 * POST     /api/contents/{id}/publish        — publish a draft
 * POST     /api/contents/{id}/archive        — archive
 * GET      /api/contents/shelf/{shelfId}     — paginated list by shelf
 * GET      /api/contents/me                  — my uploaded content
 */
@RestController
@RequestMapping("/api/contents")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    @PostMapping
    public ResponseEntity<Responses.ContentResponse> create(
            @Valid @RequestBody ContentRequests.CreateContentRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(contentService.create(req, principal.getUsername()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Responses.ContentResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(contentService.getById(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Responses.ContentResponse> update(
            @PathVariable String id,
            @RequestBody ContentRequests.UpdateContentRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(contentService.update(id, req, principal.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails principal) {
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        contentService.delete(id, principal.getUsername(), isAdmin);
        return ResponseEntity.ok(Map.of("message", "Content deleted"));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<Responses.ContentResponse> publish(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(contentService.publish(id, principal.getUsername()));
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<Responses.ContentResponse> archive(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(contentService.archive(id, principal.getUsername()));
    }

    @GetMapping("/shelf/{shelfId}")
    public ResponseEntity<Page<Responses.ContentResponse>> byShelf(
            @PathVariable String shelfId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String dir) {
        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return ResponseEntity.ok(
                contentService.getByShelf(shelfId, PageRequest.of(page, size, Sort.by(direction, sort)))
        );
    }

    @GetMapping("/me")
    public ResponseEntity<Page<Responses.ContentResponse>> myContent(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(
                contentService.getMyContent(principal.getUsername(), PageRequest.of(page, size))
        );
    }
}
