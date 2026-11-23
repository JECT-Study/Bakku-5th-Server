package com.ject.notification.domain;

import com.ject.common.event.notification.NotificationRequested;
import com.ject.common.event.notification.NotificationTargetType;
import com.ject.common.event.notification.NotificationType;
import com.ject.notification.domain.domain.ScheduledNotification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScheduledNotificationTest {

    private static final Instant FIRE_AT = Instant.parse("2026-12-01T06:30:00Z");
    private static final NotificationRequested.Target CHAT_ROOM =
            new NotificationRequested.Target(NotificationTargetType.CHAT_ROOM, 7L);

    @Test
    @DisplayName("예약 요청을 받는 사람별로 나누고, 같은 사람은 한 건만 만든다")
    void splitByReceiver() {
        // given
        NotificationRequested request = new NotificationRequested(NotificationType.EXCHANGE_CONFIRMATION,
                List.of(101L, 303L, 101L), 202L, CHAT_ROOM, Map.of("productName", "포켓몬 카드"), FIRE_AT,
                "exchange-confirmation:7");

        // when
        List<ScheduledNotification> scheduled = ScheduledNotification.from(request);

        // then
        assertEquals(List.of(101L, 303L), scheduled.stream().map(ScheduledNotification::getReceiverId).toList());
        ScheduledNotification first = scheduled.getFirst();
        assertEquals("exchange-confirmation:7", first.getScheduleKey());
        assertEquals(NotificationType.EXCHANGE_CONFIRMATION, first.getType());
        assertEquals(202L, first.getSenderId());
        assertEquals(NotificationTargetType.CHAT_ROOM, first.getTargetType());
        assertEquals(7L, first.getTargetId());
        assertEquals(Map.of("productName", "포켓몬 카드"), first.getParams());
        assertEquals(FIRE_AT, first.getFireAt());
    }

    @Test
    @DisplayName("예약 시각이 없는 요청은 예약으로 만들 수 없다")
    void requiresScheduledAt() {
        // given
        NotificationRequested request = new NotificationRequested(NotificationType.EXCHANGE_CONFIRMATION,
                List.of(101L), 202L, null, null, null, null);

        // when & then
        assertThrows(NullPointerException.class, () -> ScheduledNotification.from(request));
    }

    @Test
    @DisplayName("보낼 때는 그 사람 한 명에게 바로 보내는 요청으로 바뀐다")
    void toRequest() {
        // given
        ScheduledNotification scheduled = ScheduledNotification.restore(1L, "exchange-confirmation:7", 101L,
                NotificationType.EXCHANGE_CONFIRMATION, 202L, NotificationTargetType.CHAT_ROOM, 7L,
                Map.of("productName", "포켓몬 카드"), FIRE_AT);

        // when
        NotificationRequested request = scheduled.toRequest();

        // then
        assertEquals(new NotificationRequested(NotificationType.EXCHANGE_CONFIRMATION, List.of(101L), 202L,
                CHAT_ROOM, Map.of("productName", "포켓몬 카드"), null, null), request);
    }

    @Test
    @DisplayName("이동 대상이 없는 예약은 대상 없이 보낸다")
    void toRequestWithoutTarget() {
        // given
        ScheduledNotification scheduled = ScheduledNotification.restore(1L, null, 101L,
                NotificationType.AUTO_COMPLETE_SOON, 202L, null, null, Map.of(), FIRE_AT);

        // when & then
        assertNull(scheduled.toRequest().target());
    }
}
