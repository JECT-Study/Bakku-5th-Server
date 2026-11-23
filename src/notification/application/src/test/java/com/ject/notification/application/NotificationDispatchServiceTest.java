package com.ject.notification.application;

import com.ject.common.event.notification.NotificationRequested;
import com.ject.common.event.notification.NotificationTargetType;
import com.ject.common.event.notification.NotificationType;
import com.ject.notification.application.service.NotificationDispatchService;
import com.ject.notification.domain.domain.Notification;
import com.ject.notification.domain.domain.NotificationMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationDispatchServiceTest {

    private static final Long SENDER_ID = 202L;
    private static final NotificationRequested.Target CHAT_ROOM =
            new NotificationRequested.Target(NotificationTargetType.CHAT_ROOM, 5L);

    private FakeNotificationRepository notificationRepository;
    private NotificationDispatchService dispatchService;

    @BeforeEach
    void setUp() {
        notificationRepository = new FakeNotificationRepository();
        dispatchService = new NotificationDispatchService(notificationRepository, memberId -> "하치와레러버");
    }

    @Test
    @DisplayName("교환 요청은 받는 사람 알림 목록에 닉네임 · 제품명으로 저장한다")
    void saveExchangeRequested() {
        // when
        dispatchService.dispatch(request(NotificationType.EXCHANGE_REQUESTED, List.of(101L), SENDER_ID));

        // then
        List<Notification> saved = notificationRepository.findAll();
        assertEquals(1, saved.size());
        Notification notification = saved.getFirst();
        assertEquals(101L, notification.getReceiverId());
        assertEquals(NotificationType.EXCHANGE_REQUESTED, notification.getType());
        assertEquals("하치와레러버", notification.getTitle());
        assertEquals("[포켓몬 카드] 교환 요청이 왔어요", notification.getBody());
        assertEquals(NotificationTargetType.CHAT_ROOM, notification.getTargetType());
        assertEquals(5L, notification.getTargetId());
    }

    @Test
    @DisplayName("채팅 메시지는 알림 목록에 저장하지 않는다")
    void chatMessageIsNotSaved() {
        // when
        dispatchService.dispatch(request(NotificationType.CHAT_MESSAGE, List.of(101L), SENDER_ID));

        // then
        assertTrue(notificationRepository.findAll().isEmpty());
    }

    @Test
    @DisplayName("보낸 회원 자신은 받지 않고, 같은 사람은 한 번만 받는다")
    void excludeSenderAndDuplicates() {
        // when
        dispatchService.dispatch(request(NotificationType.APPOINTMENT_CANCELED, List.of(101L, SENDER_ID, 101L),
                SENDER_ID));

        // then
        List<Notification> saved = notificationRepository.findAll();
        assertEquals(1, saved.size());
        assertEquals(101L, saved.getFirst().getReceiverId());
    }

    @Test
    @DisplayName("보낸 회원이 없으면 제목은 앱 이름이다")
    void systemNotification() {
        // when
        dispatchService.dispatch(request(NotificationType.EXCHANGE_CONFIRMATION, List.of(101L, 202L), null));

        // then
        List<Notification> saved = notificationRepository.findAll();
        assertEquals(2, saved.size());
        assertEquals(NotificationMessages.SYSTEM_TITLE, saved.getFirst().getTitle());
    }

    @Test
    @DisplayName("문장이 없는 종류(출시 후 알림)는 저장하지 않는다")
    void unsupportedType() {
        // when
        dispatchService.dispatch(request(NotificationType.WISHLIST_KEYWORD, List.of(101L), null));

        // then
        assertTrue(notificationRepository.findAll().isEmpty());
    }

    private static NotificationRequested request(NotificationType type, List<Long> receiverIds, Long senderId) {
        return new NotificationRequested(type, receiverIds, senderId, CHAT_ROOM,
                Map.of(NotificationMessages.PRODUCT_NAME, "포켓몬 카드"), null, null);
    }
}
