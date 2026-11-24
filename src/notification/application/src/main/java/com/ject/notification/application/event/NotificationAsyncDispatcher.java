package com.ject.notification.application.event;

import com.ject.common.event.notification.NotificationRequested;
import com.ject.notification.application.service.NotificationDispatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 바로 보내는 알림(저장 · FCM 발송)을 알림 전용 스레드에서 처리한다.
 * FCM 응답을 기다리는 동안 알림을 발행한 쪽 API 응답이 늦어지지 않게 한다.
 * 실행기는 notification-infrastructure의 NotificationAsyncConfig에 있다.
 */
@Component
public class NotificationAsyncDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationAsyncDispatcher.class);

    private final NotificationDispatchService notificationDispatchService;

    public NotificationAsyncDispatcher(NotificationDispatchService notificationDispatchService) {
        this.notificationDispatchService = notificationDispatchService;
    }

    /**
     * 다른 스레드라 예외를 넘겨도 받을 곳이 없어 ERROR 로그로 남긴다 (결정 C5).
     */
    @Async("notificationExecutor")
    public void dispatch(NotificationRequested event) {
        try {
            notificationDispatchService.dispatch(event);
        } catch (RuntimeException e) {
            log.error("알림 처리 실패. type={}, receiverIds={}, senderId={}", event.type(), event.receiverIds(),
                    event.senderId(), e);
        }
    }
}
