package com.ject.notification.domain;

import com.ject.common.event.notification.NotificationTargetType;
import com.ject.common.event.notification.NotificationType;
import com.ject.notification.domain.domain.Notification;
import com.ject.notification.domain.domain.NotificationContent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NotificationTest {

    @Test
    @DisplayName("새 알림은 안 읽은 상태로 만들어진다")
    void create() {
        // given
        NotificationContent content = new NotificationContent("하치와레러버", "[포켓몬 카드] 교환 요청이 왔어요");

        // when
        Notification notification = Notification.create(101L, NotificationType.EXCHANGE_REQUESTED, content,
                NotificationTargetType.CHAT_ROOM, 5L);

        // then
        assertEquals(101L, notification.getReceiverId());
        assertEquals("하치와레러버", notification.getTitle());
        assertEquals("[포켓몬 카드] 교환 요청이 왔어요", notification.getBody());
        assertEquals(NotificationTargetType.CHAT_ROOM, notification.getTargetType());
        assertEquals(5L, notification.getTargetId());
        assertFalse(notification.isRead());
        assertNull(notification.getId());
    }

    @Test
    @DisplayName("받는 사람이 없으면 만들 수 없다")
    void createWithoutReceiver() {
        // given
        NotificationContent content = new NotificationContent("하치와레러버", "교환은 잘 하셨나요?");

        // when · then
        assertThrows(NullPointerException.class, () -> Notification.create(null,
                NotificationType.EXCHANGE_CONFIRMATION, content, null, null));
    }
}
