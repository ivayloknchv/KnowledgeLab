package com.knowledge.lab.api.service;

import com.knowledge.lab.api.dto.request.ContentRequests;
import com.knowledge.lab.api.dto.response.Responses;
import com.knowledge.lab.api.exception.ResourceNotFoundException;
import com.knowledge.lab.api.model.Content;
import com.knowledge.lab.api.model.User;
import com.knowledge.lab.api.repository.ContentRepository;
import com.knowledge.lab.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentService {

    private final ContentRepository    contentRepository;
    private final UserRepository       userRepository;
    private final ShelfService         shelfService;
    private final ContentSearchService contentSearchService;

    public Responses.ContentResponse create(ContentRequests.CreateContentRequest req, String userEmail) {
        User uploader = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        Content content = Content.builder()
                .uploaderId(uploader.getId())
                .uploaderName(uploader.getFirstName() + " " + uploader.getLastName())
                .title(req.title())
                .description(req.description())
                .type(req.type())
                .shelfId(req.shelfId())
                .author(req.author())
                .tags(req.tags() != null ? req.tags() : List.of())
                .isbn(req.isbn())
                .doi(req.doi())
                .year(req.year())
                .fileKey(req.fileKey())
                .mimeType(req.mimeType())
                .fileSizeBytes(req.fileSizeBytes())
                .coverImageUrl(req.coverImageUrl())
                .status(Content.ContentStatus.DRAFT)
                .build();

        content = contentRepository.save(content);

        if (content.getShelfId() != null) {
            shelfService.incrementContentCount(content.getShelfId());
        }

        contentSearchService.index(content);
        log.info("Created content [{}] '{}' by {}", content.getId(), content.getTitle(), userEmail);
        return Responses.ContentResponse.from(content);
    }

    public Responses.ContentResponse publish(String contentId, String userEmail) {
        Content content = findAndVerifyOwnership(contentId, userEmail);
        content.setStatus(Content.ContentStatus.PUBLISHED);
        content.setPublishedAt(Instant.now());
        content = contentRepository.save(content);
        contentSearchService.index(content);
        return Responses.ContentResponse.from(content);
    }

    public Responses.ContentResponse archive(String contentId, String userEmail) {
        Content content = findAndVerifyOwnership(contentId, userEmail);
        content.setStatus(Content.ContentStatus.ARCHIVED);
        content = contentRepository.save(content);
        contentSearchService.index(content);
        return Responses.ContentResponse.from(content);
    }

    public Responses.ContentResponse update(String contentId,
                                            ContentRequests.UpdateContentRequest req,
                                            String userEmail) {
        Content content = findAndVerifyOwnership(contentId, userEmail);
        String oldShelfId = content.getShelfId();

        if (req.title() != null)          content.setTitle(req.title());
        if (req.description() != null)    content.setDescription(req.description());
        if (req.author() != null)         content.setAuthor(req.author());
        if (req.tags() != null)           content.setTags(req.tags());
        if (req.isbn() != null)           content.setIsbn(req.isbn());
        if (req.doi() != null)            content.setDoi(req.doi());
        if (req.year() != null)           content.setYear(req.year());
        if (req.coverImageUrl() != null)  content.setCoverImageUrl(req.coverImageUrl());

        if (req.shelfId() != null && !req.shelfId().equals(oldShelfId)) {
            if (oldShelfId != null) shelfService.decrementContentCount(oldShelfId);
            content.setShelfId(req.shelfId());
            shelfService.incrementContentCount(req.shelfId());
        }

        content = contentRepository.save(content);
        contentSearchService.index(content);
        return Responses.ContentResponse.from(content);
    }

    public void delete(String contentId, String userEmail, boolean isAdmin) {
        Content content = findById(contentId);
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!isAdmin && !content.getUploaderId().equals(user.getId())) {
            throw new AccessDeniedException("You don't have permission to delete this content");
        }

        if (content.getShelfId() != null) {
            shelfService.decrementContentCount(content.getShelfId());
        }

        contentRepository.delete(content);
        contentSearchService.delete(contentId);
        log.info("Deleted content {} by {}", contentId, userEmail);
    }

    public Responses.ContentResponse getById(String contentId) {
        Content content = findById(contentId);
        content.setViewCount(content.getViewCount() + 1);
        contentRepository.save(content);
        return Responses.ContentResponse.from(content);
    }

    public Page<Responses.ContentResponse> getByShelf(String shelfId, Pageable pageable) {
        return contentRepository
                .findByShelfIdAndStatus(shelfId, Content.ContentStatus.PUBLISHED, pageable)
                .map(Responses.ContentResponse::from);
    }

    public Page<Responses.ContentResponse> getMyContent(String userEmail, Pageable pageable) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return contentRepository.findByUploaderId(user.getId(), pageable)
                .map(Responses.ContentResponse::from);
    }

    public Content findById(String contentId) {
        return contentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content", contentId));
    }

    private Content findAndVerifyOwnership(String contentId, String userEmail) {
        Content content = findById(contentId);
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!content.getUploaderId().equals(user.getId())) {
            throw new AccessDeniedException("You don't own this content");
        }
        return content;
    }
}
