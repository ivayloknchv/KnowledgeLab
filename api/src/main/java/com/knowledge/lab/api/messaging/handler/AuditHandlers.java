package com.knowledge.lab.api.messaging.handler;

import com.knowledge.lab.api.messaging.event.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Writes a structured audit trail for every annotation lifecycle event.
 *
 * Registered as multiple handlers via the generic {@link AnnotationEventHandler}
 * interface — one per event type — keeping each case small and explicit.
 *
 * In production: replace the log statements with writes to an append-only
 * audit store (e.g. a dedicated MongoDB collection, a SIEM, or CloudTrail).
 */
@Slf4j
public final class AuditHandlers {

    private AuditHandlers() {}

    @Component
    public static class OnCreated implements AnnotationEventHandler<AnnotationCreatedEvent> {
        @Override public Class<AnnotationCreatedEvent> supports() { return AnnotationCreatedEvent.class; }
        @Override
        public void handle(AnnotationCreatedEvent e) {
            log.info("[AUDIT] ANNOTATION_CREATED  id={} content={} actor={} type={} public={}",
                    e.getAnnotationId(), e.getContentId(), e.getActorId(),
                    e.getAnnotationType(), e.isPublic());
        }
    }

    @Component
    public static class OnUpdated implements AnnotationEventHandler<AnnotationUpdatedEvent> {
        @Override public Class<AnnotationUpdatedEvent> supports() { return AnnotationUpdatedEvent.class; }
        @Override
        public void handle(AnnotationUpdatedEvent e) {
            log.info("[AUDIT] ANNOTATION_UPDATED  id={} actor={}", e.getAnnotationId(), e.getActorId());
        }
    }

    @Component
    public static class OnDeleted implements AnnotationEventHandler<AnnotationDeletedEvent> {
        @Override public Class<AnnotationDeletedEvent> supports() { return AnnotationDeletedEvent.class; }
        @Override
        public void handle(AnnotationDeletedEvent e) {
            log.info("[AUDIT] ANNOTATION_DELETED  id={} actor={} byAdmin={}",
                    e.getAnnotationId(), e.getActorId(), e.isDeletedByAdmin());
        }
    }

    @Component
    public static class OnVisibilityChanged implements AnnotationEventHandler<AnnotationVisibilityChangedEvent> {
        @Override public Class<AnnotationVisibilityChangedEvent> supports() { return AnnotationVisibilityChangedEvent.class; }
        @Override
        public void handle(AnnotationVisibilityChangedEvent e) {
            log.info("[AUDIT] VISIBILITY_CHANGED  id={} actor={} was={} now={}",
                    e.getAnnotationId(), e.getActorId(), e.isWasPublic(), e.isNowPublic());
        }
    }

    @Component
    public static class OnReplyAdded implements AnnotationEventHandler<ReplyAddedEvent> {
        @Override public Class<ReplyAddedEvent> supports() { return ReplyAddedEvent.class; }
        @Override
        public void handle(ReplyAddedEvent e) {
            log.info("[AUDIT] REPLY_ADDED         annotationId={} replyId={} actor={}",
                    e.getAnnotationId(), e.getReplyId(), e.getActorId());
        }
    }

    @Component
    public static class OnReplyUpdated implements AnnotationEventHandler<ReplyUpdatedEvent> {
        @Override public Class<ReplyUpdatedEvent> supports() { return ReplyUpdatedEvent.class; }
        @Override
        public void handle(ReplyUpdatedEvent e) {
            log.info("[AUDIT] REPLY_UPDATED        annotationId={} replyId={} actor={}",
                    e.getAnnotationId(), e.getReplyId(), e.getActorId());
        }
    }

    @Component
    public static class OnReplyDeleted implements AnnotationEventHandler<ReplyDeletedEvent> {
        @Override public Class<ReplyDeletedEvent> supports() { return ReplyDeletedEvent.class; }
        @Override
        public void handle(ReplyDeletedEvent e) {
            log.info("[AUDIT] REPLY_DELETED        annotationId={} replyId={} actor={} byAdmin={}",
                    e.getAnnotationId(), e.getReplyId(), e.getActorId(), e.isDeletedByAdmin());
        }
    }
}
