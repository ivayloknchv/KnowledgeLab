package com.knowledge.lab.api.controller;

import com.knowledge.lab.api.dto.request.AnnotationRequests;
import com.knowledge.lab.api.dto.response.Responses;
import com.knowledge.lab.api.service.AnnotationService;
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
import java.util.Objects;

/**
 * Collaborative Annotation & Discussion
 *
 * POST   /api/annotations                                  — create annotation (highlight/note)
 * PATCH  /api/annotations/{id}                             — update annotation
 * DELETE /api/annotations/{id}                             — delete annotation
 * GET    /api/annotations/content/{contentId}              — all visible annotations on a content
 * GET    /api/annotations/content/{contentId}/page/{page}  — annotations on a specific page
 * GET    /api/annotations/content/{contentId}/mine         — only my annotations on a content
 *
 * -- Discussion thread (replies) --
 * POST   /api/annotations/{id}/replies                     — reply to an annotation
 * PATCH  /api/annotations/{id}/replies/{replyId}           — edit your reply
 * DELETE /api/annotations/{id}/replies/{replyId}           — delete reply
 */
@RestController
@RequestMapping("/api/annotations")
@RequiredArgsConstructor
public class AnnotationController {

    private final AnnotationService annotationService;

    @PostMapping
    public ResponseEntity<Responses.AnnotationResponse> create(
            @Valid @RequestBody AnnotationRequests.CreateAnnotationRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(annotationService.create(req, principal.getUsername()));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Responses.AnnotationResponse> update(
            @PathVariable String id,
            @RequestBody AnnotationRequests.UpdateAnnotationRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(annotationService.update(id, req, principal.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails principal) {
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        annotationService.delete(id, principal.getUsername(), isAdmin);
        return ResponseEntity.ok(Map.of("message", "Annotation deleted"));
    }

    @GetMapping("/content/{contentId}")
    public ResponseEntity<Page<Responses.AnnotationResponse>> getForContent(
            @PathVariable String contentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(
                annotationService.getForContent(contentId, principal.getUsername(),
                        PageRequest.of(page, size)));
    }

    @GetMapping("/content/{contentId}/page/{pageNumber}")
    public ResponseEntity<List<Responses.AnnotationResponse>> getForPage(
            @PathVariable String contentId,
            @PathVariable int pageNumber,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(
                annotationService.getForPage(contentId, pageNumber, principal.getUsername()));
    }

    @GetMapping("/content/{contentId}/mine")
    public ResponseEntity<List<Responses.AnnotationResponse>> getMine(
            @PathVariable String contentId,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(
                annotationService.getMyAnnotations(contentId, principal.getUsername()));
    }

    @PostMapping("/{id}/replies")
    public ResponseEntity<Responses.AnnotationResponse> addReply(
            @PathVariable String id,
            @Valid @RequestBody AnnotationRequests.AddReplyRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(annotationService.addReply(id, req, principal.getUsername()));
    }

    @PatchMapping("/{id}/replies/{replyId}")
    public ResponseEntity<Responses.AnnotationResponse> updateReply(
            @PathVariable String id,
            @PathVariable String replyId,
            @Valid @RequestBody AnnotationRequests.AddReplyRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(
                annotationService.updateReply(id, replyId, req, principal.getUsername()));
    }

    @DeleteMapping("/{id}/replies/{replyId}")
    public ResponseEntity<Responses.AnnotationResponse> deleteReply(
            @PathVariable String id,
            @PathVariable String replyId,
            @AuthenticationPrincipal UserDetails principal) {
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_ADMIN"));
        return ResponseEntity.ok(
                annotationService.deleteReply(id, replyId, principal.getUsername(), isAdmin));
    }
}
