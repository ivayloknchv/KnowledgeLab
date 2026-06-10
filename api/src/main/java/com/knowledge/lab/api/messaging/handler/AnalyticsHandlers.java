package com.knowledge.lab.api.messaging.handler;

import com.knowledge.lab.api.messaging.event.AnnotationCreatedEvent;
import com.knowledge.lab.api.messaging.event.ReplyAddedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Captures annotation engagement signals useful for the recommendation engine.
 *
 * Events processed here:
 *  - {@link AnnotationCreatedEvent} → "user found this content interesting enough to annotate"
 *  - {@link ReplyAddedEvent}        → "this annotation sparked discussion — high-value content"
 *
 * In production: forward to your analytics pipeline (Segment, Mixpanel, custom ML feature store).
 */
@Slf4j
public final class AnalyticsHandlers {

    private AnalyticsHandlers() {}

    @Component
    public static class OnAnnotationCreated implements AnnotationEventHandler<AnnotationCreatedEvent> {

        @Override
        public Class<AnnotationCreatedEvent> supports() {
            return AnnotationCreatedEvent.class;
        }

        @Override
        public void handle(AnnotationCreatedEvent event) {
            log.info(
                    "[ANALYTICS] Annotation engagement: user='{}' content='{}' type='{}' public='{}'",
                    event.getActorId(), event.getContentId(),
                    event.getAnnotationType(), event.isPublic()
            );
            /*
             * TODO: Emit to ML feature store or analytics service.
             * Example:
             *
             *   analyticsClient.track(EngagementEvent.builder()
             *       .userId(event.getActorId())
             *       .contentId(event.getContentId())
             *       .signal(EngagementSignal.ANNOTATED)
             *       .weight(event.isPublic() ? 1.5 : 1.0)  // public = stronger signal
             *       .build());
             */
        }
    }

    @Component
    public static class OnReplyAdded implements AnnotationEventHandler<ReplyAddedEvent> {

        @Override
        public Class<ReplyAddedEvent> supports() {
            return ReplyAddedEvent.class;
        }

        @Override
        public void handle(ReplyAddedEvent event) {
            log.info(
                    "[ANALYTICS] Discussion signal: annotation='{}' on content='{}' by user='{}'",
                    event.getAnnotationId(), event.getContentId(), event.getActorId()
            );
            /*
             * TODO: Boost this content's collaborative score in the recommendation engine.
             */
        }
    }
}
