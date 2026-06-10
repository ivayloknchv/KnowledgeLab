package com.knowledge.lab.api.messaging.handler;

import com.knowledge.lab.api.messaging.event.AnnotationVisibilityChangedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Handles {@link AnnotationVisibilityChangedEvent} — broadcasts to content
 * collaborators when a private annotation is made public (shared).
 *
 * "Someone shared a highlight on [content]" — the collaborative dialogue moment.
 */
@Slf4j
@Component
public class AnnotationSharedNotificationHandler
        implements AnnotationEventHandler<AnnotationVisibilityChangedEvent> {

    @Override
    public Class<AnnotationVisibilityChangedEvent> supports() {
        return AnnotationVisibilityChangedEvent.class;
    }

    @Override
    public void handle(AnnotationVisibilityChangedEvent event) {
        if (!event.isNowPublic()) {
            // Annotation was made private — no broadcast needed.
            return;
        }

        log.info(
                "[NOTIFY] '{}' shared an annotation on content '{}'. Broadcasting to collaborators.",
                event.getActorName(),
                event.getContentId()
        );

        /*
         * TODO: broadcast to all users currently reading this content.
         * Example with WebSocket:
         *
         *   messagingTemplate.convertAndSend(
         *       "/topic/content/" + event.getContentId() + "/annotations",
         *       AnnotationSharedPayload.from(event)
         *   );
         */
    }
}
