package com.ject.notification.application;

import com.ject.common.core.time.ServiceTime;
import com.ject.common.event.notification.NotificationTargetType;
import com.ject.common.event.notification.NotificationType;
import com.ject.notification.application.dto.NotificationPage;
import com.ject.notification.application.dto.NotificationPage.NotificationItem;
import com.ject.notification.application.service.NotificationQueryService;
import com.ject.notification.domain.domain.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationQueryServiceTest {

    private static final Long MEMBER_ID = 101L;
    private static final Instant NOW = Instant.parse("2026-12-01T00:00:00Z");

    private FakeNotificationRepository notificationRepository;
    private NotificationQueryService queryService;

    @BeforeEach
    void setUp() {
        notificationRepository = new FakeNotificationRepository();
        queryService = new NotificationQueryService(notificationRepository,
                new ServiceTime(Clock.fixed(NOW, ZoneId.of("Asia/Seoul"))));
    }

    @Test
    @DisplayName("NT-1 내 알림을 최신순으로 주고, 더 있으면 마지막 알림 id를 다음 커서로 준다")
    void firstPage() {
        // given
        add(1L, MEMBER_ID, NOW.minusSeconds(300), null);
        add(2L, MEMBER_ID, NOW.minusSeconds(200), NOW);
        add(3L, MEMBER_ID, NOW.minusSeconds(100), null);
        add(4L, 202L, NOW.minusSeconds(50), null);

        // when
        NotificationPage page = queryService.getNotifications(MEMBER_ID, null, 2);

        // then
        assertEquals(List.of(3L, 2L), page.content().stream().map(NotificationItem::notificationId).toList());
        assertTrue(page.hasNext());
        assertEquals(2L, page.nextCursorId());
        NotificationItem first = page.content().getFirst();
        assertEquals(NotificationType.EXCHANGE_REQUESTED, first.type());
        assertEquals("하치와레러버", first.title());
        assertEquals(NotificationTargetType.CHAT_ROOM, first.targetType());
        assertEquals(5L, first.targetId());
        assertFalse(first.read());
        assertTrue(page.content().get(1).read());
    }

    @Test
    @DisplayName("NT-1 커서 다음부터 주고, 마지막 페이지면 다음 커서가 없다")
    void nextPage() {
        // given
        add(1L, MEMBER_ID, NOW.minusSeconds(300), null);
        add(2L, MEMBER_ID, NOW.minusSeconds(200), null);
        add(3L, MEMBER_ID, NOW.minusSeconds(100), null);

        // when
        NotificationPage page = queryService.getNotifications(MEMBER_ID, 2L, 2);

        // then
        assertEquals(List.of(1L), page.content().stream().map(NotificationItem::notificationId).toList());
        assertFalse(page.hasNext());
        assertNull(page.nextCursorId());
    }

    @Test
    @DisplayName("NT-1 30일 지난 알림은 삭제 전이라도 보이지 않는다")
    void olderThan30DaysIsHidden() {
        // given
        add(1L, MEMBER_ID, NOW.minus(Duration.ofDays(30)).minusSeconds(1), null);
        add(2L, MEMBER_ID, NOW.minus(Duration.ofDays(30)), null);

        // when
        NotificationPage page = queryService.getNotifications(MEMBER_ID, null, 20);

        // then
        assertEquals(List.of(2L), page.content().stream().map(NotificationItem::notificationId).toList());
    }

    @Test
    @DisplayName("NT-2 30일 안에 받은 내 알림 중 안 읽은 것만 센다")
    void unreadCount() {
        // given
        add(1L, MEMBER_ID, NOW.minus(Duration.ofDays(30)).minusSeconds(1), null);
        add(2L, MEMBER_ID, NOW.minusSeconds(200), NOW);
        add(3L, MEMBER_ID, NOW.minusSeconds(100), null);
        add(4L, MEMBER_ID, NOW.minus(Duration.ofDays(30)), null);
        add(5L, 202L, NOW.minusSeconds(50), null);

        // when
        long count = queryService.getUnreadCount(MEMBER_ID);

        // then
        assertEquals(2, count);
    }

    private void add(Long id, Long receiverId, Instant createdAt, Instant readAt) {
        notificationRepository.add(Notification.restore(id, receiverId, NotificationType.EXCHANGE_REQUESTED,
                "하치와레러버", "[포켓몬 카드] 교환 요청이 왔어요", NotificationTargetType.CHAT_ROOM, 5L, readAt,
                createdAt));
    }
}
