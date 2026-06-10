package com.knowledge.lab.api.controller;

import com.knowledge.lab.api.dto.request.ShelfRequests;
import com.knowledge.lab.api.dto.response.Responses;
import com.knowledge.lab.api.service.ShelfService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Library Shelves — organize content into categories
 *
 * GET      /api/shelves             — list all public shelves (paginated)
 * GET      /api/shelves/system      — list global system shelves
 * GET      /api/shelves/me          — list current user's personal shelves
 * GET      /api/shelves/{id}        — get shelf details
 * POST     /api/shelves             — create a personal shelf
 * PATCH    /api/shelves/{id}        — update your shelf
 * DELETE   /api/shelves/{id}        — delete your shelf
 */
@RestController
@RequestMapping("/api/shelves")
@RequiredArgsConstructor
public class ShelfController {

    private final ShelfService shelfService;

    @GetMapping
    public ResponseEntity<Page<Responses.ShelfResponse>> listPublic(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(shelfService.listPublicShelves(PageRequest.of(page, size)));
    }

    @GetMapping("/system")
    public ResponseEntity<List<Responses.ShelfResponse>> listSystem() {
        return ResponseEntity.ok(shelfService.listSystemShelves());
    }

    @GetMapping("/me")
    public ResponseEntity<List<Responses.ShelfResponse>> listMine(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(shelfService.listMyShelves(principal.getUsername()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Responses.ShelfResponse> get(@PathVariable String id) {
        return ResponseEntity.ok(shelfService.getShelf(id));
    }

    @PostMapping
    public ResponseEntity<Responses.ShelfResponse> create(
            @Valid @RequestBody ShelfRequests.CreateShelfRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(shelfService.createShelf(req, principal.getUsername()));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Responses.ShelfResponse> update(
            @PathVariable String id,
            @RequestBody ShelfRequests.UpdateShelfRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(shelfService.updateShelf(id, req, principal.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails principal) {
        shelfService.deleteShelf(id, principal.getUsername());
        return ResponseEntity.ok(Map.of("message", "Shelf deleted"));
    }
}
