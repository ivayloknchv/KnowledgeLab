package com.knowledge.lab.api.messaging.handler;

import com.knowledge.lab.api.messaging.event.ReplyAddedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Handles {@link ReplyAddedEvent} — notifies the annotation author that
 * someone has joined their discussion thread.
 *
 * Currently logs the notification; replace the body with your preferred
 * delivery mechanism (WebSocket push, FCM, email service, etc.) without
 * touching any other class.
 */
@Slf4j
@Component
public class ReplyNotificationHandler implements AnnotationEventHandler<ReplyAddedEvent> {

    @Override
    public Class<ReplyAddedEvent> supports() {
        return ReplyAddedEvent.class;
    }

    @Override
    public void handle(ReplyAddedEvent event) {
        if (event.getAnnotationAuthorId().equals(event.getActorId())) {
            // Author replied to their own annotation — no notification needed.
            return;
        }

        log.info(
                "[NOTIFY] User '{}' replied to annotation '{}' on content '{}'. " +
                        "Notifying annotation author '{}'.",
                event.getActorName(),
                event.getAnnotationId(),
                event.getContentId(),
                event.getAnnotationAuthorId()
        );

        /*
         * TODO: wire a NotificationService / WebSocket SimpMessagingTemplate here.
         * Example:
         *
         *   notificationService.sendToUser(
         *       event.getAnnotationAuthorId(),
         *       Notification.builder()
         *           .type(NotificationType.REPLY_ADDED)
         *           .message(event.getActorName() + " replied to your annotation")
         *           .referenceId(event.getAnnotationId())
         *           .build()
         *   );
         */
    }
}
