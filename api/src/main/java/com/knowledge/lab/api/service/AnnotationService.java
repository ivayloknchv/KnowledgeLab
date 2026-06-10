package com.knowledge.lab.api.service;

import com.knowledge.lab.api.dto.request.AnnotationRequests;
import com.knowledge.lab.api.dto.response.Responses;
import com.knowledge.lab.api.exception.ResourceNotFoundException;
import com.knowledge.lab.api.messaging.event.*;
import com.knowledge.lab.api.messaging.publisher.AnnotationEventPublisher;
import com.knowledge.lab.api.model.Annotation;
import com.knowledge.lab.api.model.User;
import com.knowledge.lab.api.repository.AnnotationRepository;
import com.knowledge.lab.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Core annotation business logic.
 *
 * Kafka is invisible here — this service publishes domain events through
 * the {@link AnnotationEventPublisher} port only. The transport (Kafka,
 * in-process, test double) is decided at wiring time by Spring.
 *
 * Pattern: save → publish. The event is raised after a successful persist
 * so we never broadcast a state that was never committed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnnotationService {

    private final AnnotationRepository      annotationRepository;
    private final UserRepository            userRepository;
    private final ContentService            contentService;
    private final AnnotationEventPublisher  eventPublisher;       // ← only coupling to messaging

    public Responses.AnnotationResponse create(AnnotationRequests.CreateAnnotationRequest req,
                                               String userEmail) {
        contentService.findById(req.contentId());
        User author = resolveUser(userEmail);

        Annotation annotation = Annotation.builder()
                .contentId(req.contentId())
                .pageNumber(req.pageNumber())
                .range(req.range())
                .selectedText(req.selectedText())
                .authorId(author.getId())
                .authorName(displayName(author))
                .type(req.type())
                .color(req.color() != null ? req.color() : "#FFD700")
                .body(req.body())
                .isPublic(req.isPublic())
                .build();

        annotation = annotationRepository.save(annotation);
        log.info("Annotation created [id={}] on content={} by {}", annotation.getId(), req.contentId(), userEmail);

        eventPublisher.publish(AnnotationCreatedEvent.builder()
                .annotationId(annotation.getId())
                .contentId(annotation.getContentId())
                .actorId(author.getId())
                .actorName(displayName(author))
                .annotationType(annotation.getType())
                .pageNumber(annotation.getPageNumber())
                .selectedText(annotation.getSelectedText())
                .body(annotation.getBody())
                .isPublic(annotation.isPublic())
                .build());

        return Responses.AnnotationResponse.from(annotation);
    }

    public Responses.AnnotationResponse update(String annotationId,
                                               AnnotationRequests.UpdateAnnotationRequest req,
                                               String userEmail) {
        User actor      = resolveUser(userEmail);
        Annotation ann  = findAndVerifyOwnership(annotationId, actor.getId());

        boolean wasPublic = ann.isPublic();

        if (req.body() != null)     ann.setBody(req.body());
        if (req.color() != null)    ann.setColor(req.color());
        if (req.isPublic() != null) ann.setPublic(req.isPublic());

        ann = annotationRepository.save(ann);

        // Emit a fine-grained body/colour update event
        eventPublisher.publish(AnnotationUpdatedEvent.builder()
                .annotationId(ann.getId())
                .contentId(ann.getContentId())
                .actorId(actor.getId())
                .actorName(displayName(actor))
                .newBody(ann.getBody())
                .newColor(ann.getColor())
                .build());

        // Emit a separate visibility event if the flag was flipped
        if (req.isPublic() != null && req.isPublic() != wasPublic) {
            eventPublisher.publish(AnnotationVisibilityChangedEvent.builder()
                    .annotationId(ann.getId())
                    .contentId(ann.getContentId())
                    .actorId(actor.getId())
                    .actorName(displayName(actor))
                    .wasPublic(wasPublic)
                    .isNowPublic(ann.isPublic())
                    .build());
        }

        return Responses.AnnotationResponse.from(ann);
    }

    public void delete(String annotationId, String userEmail, boolean isAdmin) {
        Annotation ann = findById(annotationId);
        User actor     = resolveUser(userEmail);

        if (!isAdmin && !ann.getAuthorId().equals(actor.getId())) {
            throw new AccessDeniedException("You don't own this annotation");
        }

        annotationRepository.delete(ann);

        eventPublisher.publish(AnnotationDeletedEvent.builder()
                .annotationId(ann.getId())
                .contentId(ann.getContentId())
                .actorId(actor.getId())
                .actorName(displayName(actor))
                .deletedByAdmin(isAdmin)
                .build());
    }

    public Page<Responses.AnnotationResponse> getForContent(String contentId,
                                                            String requesterEmail,
                                                            Pageable pageable) {
        contentService.findById(contentId);
        String requesterId = resolveUser(requesterEmail).getId();
        return annotationRepository.findVisibleByContentId(contentId, requesterId, pageable)
                .map(Responses.AnnotationResponse::from);
    }

    public List<Responses.AnnotationResponse> getForPage(String contentId,
                                                         int pageNumber,
                                                         String requesterEmail) {
        String requesterId = resolveUser(requesterEmail).getId();
        return annotationRepository
                .findVisibleByContentIdAndPageNumber(contentId, pageNumber, requesterId)
                .stream().map(Responses.AnnotationResponse::from).toList();
    }

    public List<Responses.AnnotationResponse> getMyAnnotations(String contentId, String userEmail) {
        String userId = resolveUser(userEmail).getId();
        return annotationRepository.findByContentIdAndAuthorId(contentId, userId)
                .stream().map(Responses.AnnotationResponse::from).toList();
    }

    public Responses.AnnotationResponse addReply(String annotationId,
                                                 AnnotationRequests.AddReplyRequest req,
                                                 String userEmail) {
        Annotation ann = findById(annotationId);
        User author    = resolveUser(userEmail);

        if (!ann.isPublic() && !ann.getAuthorId().equals(author.getId())) {
            throw new AccessDeniedException("This annotation is private");
        }

        Annotation.Reply reply = Annotation.Reply.builder()
                .id(UUID.randomUUID().toString())
                .authorId(author.getId())
                .authorName(displayName(author))
                .body(req.body())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        ann.getReplies().add(reply);
        ann = annotationRepository.save(ann);
        log.info("Reply [id={}] added to annotation {} by {}", reply.getId(), annotationId, userEmail);

        eventPublisher.publish(ReplyAddedEvent.builder()
                .annotationId(ann.getId())
                .contentId(ann.getContentId())
                .actorId(author.getId())
                .actorName(displayName(author))
                .replyId(reply.getId())
                .replyBody(reply.getBody())
                .annotationAuthorId(ann.getAuthorId())
                .build());

        return Responses.AnnotationResponse.from(ann);
    }

    public Responses.AnnotationResponse updateReply(String annotationId,
                                                    String replyId,
                                                    AnnotationRequests.AddReplyRequest req,
                                                    String userEmail) {
        Annotation ann = findById(annotationId);
        User actor     = resolveUser(userEmail);

        ann.getReplies().stream()
                .filter(r -> r.getId().equals(replyId))
                .findFirst()
                .ifPresentOrElse(reply -> {
                    if (!reply.getAuthorId().equals(actor.getId())) {
                        throw new AccessDeniedException("You don't own this reply");
                    }
                    reply.setBody(req.body());
                    reply.setUpdatedAt(Instant.now());
                }, () -> {
                    throw new ResourceNotFoundException("Reply not found: " + replyId);
                });

        ann = annotationRepository.save(ann);

        eventPublisher.publish(ReplyUpdatedEvent.builder()
                .annotationId(ann.getId())
                .contentId(ann.getContentId())
                .actorId(actor.getId())
                .actorName(displayName(actor))
                .replyId(replyId)
                .newBody(req.body())
                .build());

        return Responses.AnnotationResponse.from(ann);
    }

    public Responses.AnnotationResponse deleteReply(String annotationId,
                                                    String replyId,
                                                    String userEmail,
                                                    boolean isAdmin) {
        Annotation ann = findById(annotationId);
        User actor     = resolveUser(userEmail);

        boolean removed = ann.getReplies().removeIf(r -> {
            if (!r.getId().equals(replyId)) return false;
            if (!isAdmin && !r.getAuthorId().equals(actor.getId())) {
                throw new AccessDeniedException("You don't own this reply");
            }
            return true;
        });

        if (!removed) throw new ResourceNotFoundException("Reply not found: " + replyId);

        ann = annotationRepository.save(ann);

        eventPublisher.publish(ReplyDeletedEvent.builder()
                .annotationId(ann.getId())
                .contentId(ann.getContentId())
                .actorId(actor.getId())
                .actorName(displayName(actor))
                .replyId(replyId)
                .deletedByAdmin(isAdmin)
                .build());

        return Responses.AnnotationResponse.from(ann);
    }

    private Annotation findById(String id) {
        return annotationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Annotation", id));
    }

    private Annotation findAndVerifyOwnership(String id, String userId) {
        Annotation ann = findById(id);
        if (!ann.getAuthorId().equals(userId)) {
            throw new AccessDeniedException("You don't own this annotation");
        }
        return ann;
    }

    private User resolveUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private static String displayName(User user) {
        return user.getFirstName() + " " + user.getLastName();
    }
}
