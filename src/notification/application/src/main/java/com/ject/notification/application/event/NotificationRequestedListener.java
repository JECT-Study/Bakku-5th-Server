package com.ject.notification.application.event;

import com.ject.common.event.notification.NotificationRequested;
import com.ject.notification.application.service.NotificationScheduleService;
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

    private final NotificationAsyncDispatcher notificationAsyncDispatcher;
    private final NotificationScheduleService notificationScheduleService;

    public NotificationRequestedListener(NotificationAsyncDispatcher notificationAsyncDispatcher,
                                         NotificationScheduleService notificationScheduleService) {
        this.notificationAsyncDispatcher = notificationAsyncDispatcher;
        this.notificationScheduleService = notificationScheduleService;
    }

    /**
     * 요청한 쪽 트랜잭션이 커밋된 뒤 처리하고, 트랜잭션 밖(배치 등)에서 발행하면 바로 처리한다.
     * 예약 시각이 있으면 예약해 두고(같은 키면 교체), 없으면 바로 보낸다.
     * 바로 보내는 요청에 예약 키가 있으면 그 키의 예약을 먼저 취소한다 (예: 약속 취소 → 교환 확인 예약 취소).
     * <p>
     * 예약 · 취소는 DB만 바꾸는 짧은 작업이라 발행한 순서대로 여기서 바로 처리한다
     * (다른 스레드로 넘기면 "취소 → 다시 예약"이 뒤바뀌어 새 예약이 지워질 수 있음).
     * FCM을 기다리는 바로 보내기만 알림 전용 스레드로 넘긴다.
     * 실패해도 요청한 쪽 응답에는 영향이 없어야 해서 예외는 넘기지 않고 ERROR 로그로 남긴다 (결정 C5).
     */
    @TransactionalEventListener(fallbackExecution = true)
    public void handle(NotificationRequested event) {
        if (event.scheduledAt() != null) {
            schedule(event);
            return;
        }
        if (event.scheduleKey() != null) {
            cancel(event);
        }
        notificationAsyncDispatcher.dispatch(event);
    }

    private void schedule(NotificationRequested event) {
        try {
            notificationScheduleService.schedule(event);
        } catch (RuntimeException e) {
            log.error("예약 알림 저장 실패. type={}, receiverIds={}, scheduleKey={}", event.type(), event.receiverIds(),
                    event.scheduleKey(), e);
        }
    }

    /**
     * 취소가 실패해도 바로 보낼 알림(예: 약속 취소)은 보낸다.
     */
    private void cancel(NotificationRequested event) {
        try {
            notificationScheduleService.cancel(event.scheduleKey());
        } catch (RuntimeException e) {
            log.error("예약 알림 취소 실패. type={}, scheduleKey={}", event.type(), event.scheduleKey(), e);
        }
    }
}
