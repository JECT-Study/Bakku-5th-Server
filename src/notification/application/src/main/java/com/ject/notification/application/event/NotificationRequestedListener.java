package com.ject.notification.application.event;

import com.ject.common.event.notification.NotificationRequested;
import com.ject.notification.application.service.NotificationDispatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 다른 파트가 발행한 알림 요청을 받는다 (결정 B2).
 */
@Component
public class NotificationRequestedListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationRequestedListener.class);

    private final NotificationDispatchService notificationDispatchService;

    public NotificationRequestedListener(NotificationDispatchService notificationDispatchService) {
        this.notificationDispatchService = notificationDispatchService;
    }

    /**
     * 요청한 쪽 트랜잭션이 커밋된 뒤 처리하고, 트랜잭션 밖(배치 등)에서 발행하면 바로 처리한다.
     * 실패해도 요청한 쪽 응답에는 영향이 없어야 해서 예외는 넘기지 않고 ERROR 로그로 남긴다 (결정 C5).
     * 예약 알림(scheduledAt 있음)은 여기서 보내지 않는다.
     */
    @TransactionalEventListener(fallbackExecution = true)
    public void handle(NotificationRequested event) {
        if (event.scheduledAt() != null) {
            return;
        }
        try {
            notificationDispatchService.dispatch(event);
        } catch (RuntimeException e) {
            log.error("알림 처리 실패. type={}, receiverIds={}, senderId={}", event.type(), event.receiverIds(),
                    event.senderId(), e);
        }
    }
}
