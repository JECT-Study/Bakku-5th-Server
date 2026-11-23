package com.ject.notification.domain;

import com.ject.common.event.notification.NotificationType;
import com.ject.notification.domain.domain.NotificationContent;
import com.ject.notification.domain.domain.NotificationMessages;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationMessagesTest {

    @Test
    @DisplayName("제목은 보낸 회원 닉네임, 내용은 제품명과 종류별 문장이다")
    void content() {
        // when
        NotificationContent content = NotificationMessages.content(NotificationType.EXCHANGE_REQUESTED,
                "하치와레러버", "포켓몬 카드");

        // then
        assertEquals("하치와레러버", content.title());
        assertEquals("[포켓몬 카드] 교환 요청이 왔어요", content.body());
    }

    @Test
    @DisplayName("보낸 회원이 없으면 제목은 앱 이름, 제품명이 없으면 문장만 쓴다")
    void contentWithoutSenderAndProduct() {
        // when
        NotificationContent content = NotificationMessages.content(NotificationType.EXCHANGE_CONFIRMATION,
                null, " ");

        // then
        assertEquals(NotificationMessages.SYSTEM_TITLE, content.title());
        assertEquals("교환은 잘 하셨나요?", content.body());
    }

    @Test
    @DisplayName("채팅 메시지만 알림 목록에 저장하지 않는다")
    void savesToList() {
        assertFalse(NotificationMessages.savesToList(NotificationType.CHAT_MESSAGE));
        assertTrue(NotificationMessages.savesToList(NotificationType.APPOINTMENT_PROPOSED));
        assertTrue(NotificationMessages.savesToList(NotificationType.POST_UPDATED));
    }

    @Test
    @DisplayName("운영 안내 · 출시 후 알림은 아직 문장이 없어 보내지 않는다")
    void unsupported() {
        assertFalse(NotificationMessages.isSupported(NotificationType.OPERATION_NOTICE));
        assertFalse(NotificationMessages.isSupported(NotificationType.WISHLIST_KEYWORD));
        assertThrows(IllegalArgumentException.class,
                () -> NotificationMessages.content(NotificationType.WISHLIST_KEYWORD, null, null));
    }
}
