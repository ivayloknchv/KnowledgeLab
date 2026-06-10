package com.knowledge.lab.api.controller;

import com.knowledge.lab.api.dto.request.ReadingListRequests;
import com.knowledge.lab.api.dto.response.Responses;
import com.knowledge.lab.api.service.ReadingListService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Reading Lists & Bookmarks — personal curated collections
 *
 * GET      /api/reading-lists                                — list my reading lists
 * POST     /api/reading-lists                                — create a reading list
 * GET      /api/reading-lists/{id}                           — get list with all entries
 * DELETE   /api/reading-lists/{id}                           — delete list
 * POST     /api/reading-lists/{id}/bookmarks                 — add a bookmark
 * PATCH    /api/reading-lists/{id}/bookmarks/{contentId}     — update bookmark (mark read / note)
 * DELETE   /api/reading-lists/{id}/bookmarks/{contentId}     — remove bookmark
 */
@RestController
@RequestMapping("/api/reading-lists")
@RequiredArgsConstructor
public class ReadingListController {

    private final ReadingListService readingListService;

    @GetMapping
    public ResponseEntity<List<Responses.ReadingListResponse>> listMine(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(readingListService.listMine(principal.getUsername()));
    }

    @PostMapping
    public ResponseEntity<Responses.ReadingListResponse> create(
            @Valid @RequestBody ReadingListRequests.CreateReadingListRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(readingListService.create(req, principal.getUsername()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Responses.ReadingListDetailResponse> get(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(readingListService.getDetail(id, principal.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails principal) {
        readingListService.delete(id, principal.getUsername());
        return ResponseEntity.ok(Map.of("message", "Reading list deleted"));
    }

    @PostMapping("/{id}/bookmarks")
    public ResponseEntity<Responses.ReadingListDetailResponse> addBookmark(
            @PathVariable String id,
            @Valid @RequestBody ReadingListRequests.AddBookmarkRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(readingListService.addBookmark(id, req, principal.getUsername()));
    }

    @PatchMapping("/{id}/bookmarks/{contentId}")
    public ResponseEntity<Responses.ReadingListDetailResponse> updateBookmark(
            @PathVariable String id,
            @PathVariable String contentId,
            @RequestBody ReadingListRequests.UpdateBookmarkRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(
                readingListService.updateBookmark(id, contentId, req, principal.getUsername()));
    }

    @DeleteMapping("/{id}/bookmarks/{contentId}")
    public ResponseEntity<Responses.ReadingListDetailResponse> removeBookmark(
            @PathVariable String id,
            @PathVariable String contentId,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(
                readingListService.removeBookmark(id, contentId, principal.getUsername()));
    }
}
