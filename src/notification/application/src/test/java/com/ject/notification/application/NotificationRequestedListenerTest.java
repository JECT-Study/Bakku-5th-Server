package com.ject.notification.application;

import com.ject.common.event.notification.NotificationRequested;
import com.ject.common.event.notification.NotificationType;
import com.ject.notification.application.event.NotificationRequestedListener;
import com.ject.notification.application.service.NotificationDispatchService;
import com.ject.notification.domain.domain.Notification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationRequestedListenerTest {

    @Test
    @DisplayName("알림 요청을 받으면 알림 목록에 저장한다")
    void handle() {
        // given
        FakeNotificationRepository notificationRepository = new FakeNotificationRepository();
        NotificationRequestedListener listener = listener(notificationRepository);

        // when
        listener.handle(request(null));

        // then
        assertEquals(1, notificationRepository.findAll().size());
    }

    @Test
    @DisplayName("예약 알림은 여기서 보내지 않는다")
    void scheduledIsSkipped() {
        // given
        FakeNotificationRepository notificationRepository = new FakeNotificationRepository();
        NotificationRequestedListener listener = listener(notificationRepository);

        // when
        listener.handle(request(Instant.parse("2026-12-01T06:30:00Z")));

        // then
        assertTrue(notificationRepository.findAll().isEmpty());
    }

    @Test
    @DisplayName("처리하다 실패해도 예외를 넘기지 않는다 (요청한 쪽은 이미 커밋됨)")
    void failureIsNotPropagated() {
        // given
        FakeNotificationRepository notificationRepository = new FakeNotificationRepository() {
            @Override
            public void saveAll(List<Notification> notifications) {
                throw new IllegalStateException("DB 오류");
            }
        };
        NotificationRequestedListener listener = listener(notificationRepository);

        // when & then
        assertDoesNotThrow(() -> listener.handle(request(null)));
    }

    private static NotificationRequestedListener listener(FakeNotificationRepository notificationRepository) {
        return new NotificationRequestedListener(
                new NotificationDispatchService(notificationRepository, memberId -> "하치와레러버"));
    }

    private static NotificationRequested request(Instant scheduledAt) {
        String scheduleKey = scheduledAt == null ? null : "exchange-confirmation:1";
        return new NotificationRequested(NotificationType.EXCHANGE_CONFIRMATION, List.of(101L), 202L, null, null,
                scheduledAt, scheduleKey);
    }
}
