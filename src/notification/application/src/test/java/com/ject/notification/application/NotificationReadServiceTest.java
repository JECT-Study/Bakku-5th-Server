package com.ject.notification.application;

import com.ject.common.core.exception.DomainException;
import com.ject.common.core.time.ServiceTime;
import com.ject.common.event.notification.NotificationTargetType;
import com.ject.common.event.notification.NotificationType;
import com.ject.notification.application.service.NotificationReadService;
import com.ject.notification.domain.domain.Notification;
import com.ject.notification.domain.exception.NotificationErrorCode;
import com.ject.notification.domain.exception.NotificationNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NotificationReadServiceTest {

    private static final Long MEMBER_ID = 101L;
    private static final Instant NOW = Instant.parse("2026-12-01T00:00:00Z");

    private FakeNotificationRepository notificationRepository;
    private NotificationReadService readService;

    @BeforeEach
    void setUp() {
        notificationRepository = new FakeNotificationRepository();
        readService = new NotificationReadService(notificationRepository,
                new ServiceTime(Clock.fixed(NOW, ZoneId.of("Asia/Seoul"))));
    }

    @Test
    @DisplayName("NT-3 안 읽은 알림을 지금 시각으로 읽음 처리한다")
    void read() {
        // given
        add(1L, MEMBER_ID, NOW.minusSeconds(100), null);

        // when
        readService.read(MEMBER_ID, 1L);

        // then
        assertEquals(NOW, find(1L).getReadAt());
    }

    @Test
    @DisplayName("NT-3 이미 읽은 알림이어도 성공이고, 처음 읽은 시각은 그대로다")
    void alreadyRead() {
        // given
        Instant firstReadAt = NOW.minusSeconds(50);
        add(1L, MEMBER_ID, NOW.minusSeconds(100), firstReadAt);

        // when & then
        assertDoesNotThrow(() -> readService.read(MEMBER_ID, 1L));
        assertEquals(firstReadAt, find(1L).getReadAt());
    }

    @Test
    @DisplayName("NT-3 없는 알림이면 NOTIFICATION_001")
    void notFound() {
        assertNotFound(999L);
    }

    @Test
    @DisplayName("NT-3 남의 알림이면 NOTIFICATION_001이고 읽음 처리하지 않는다")
    void othersNotification() {
        // given
        add(1L, 202L, NOW.minusSeconds(100), null);

        // when & then
        assertNotFound(1L);
        assertEquals(null, find(1L).getReadAt());
    }

    @Test
    @DisplayName("NT-3 30일 지난 알림은 삭제 전이라도 NOTIFICATION_001")
    void olderThan30Days() {
        // given
        add(1L, MEMBER_ID, NOW.minus(Duration.ofDays(30)).minusSeconds(1), null);

        // when & then
        assertNotFound(1L);
    }

    private void assertNotFound(Long notificationId) {
        DomainException e = assertThrows(NotificationNotFoundException.class,
                () -> readService.read(MEMBER_ID, notificationId));
        assertEquals(NotificationErrorCode.NOTIFICATION_NOT_FOUND, e.getErrorCode());
    }

    private void add(Long id, Long receiverId, Instant createdAt, Instant readAt) {
        notificationRepository.add(Notification.restore(id, receiverId, NotificationType.EXCHANGE_REQUESTED,
                "하치와레러버", "[포켓몬 카드] 교환 요청이 왔어요", NotificationTargetType.CHAT_ROOM, 5L, readAt,
                createdAt));
    }

    private Notification find(Long id) {
        return notificationRepository.findAll().stream()
                .filter(notification -> notification.getId().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
