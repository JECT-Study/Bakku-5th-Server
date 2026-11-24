package com.ject.notification.application;

import com.ject.common.event.notification.NotificationRequested;
import com.ject.common.event.notification.NotificationTargetType;
import com.ject.common.event.notification.NotificationType;
import com.ject.notification.application.dto.PushMessage;
import com.ject.notification.application.port.PushTokenPort;
import com.ject.notification.application.service.NotificationDispatchService;
import com.ject.notification.domain.domain.Notification;
import com.ject.notification.domain.domain.NotificationMessages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationDispatchServiceTest {

    private static final Long SENDER_ID = 202L;
    private static final NotificationRequested.Target CHAT_ROOM =
            new NotificationRequested.Target(NotificationTargetType.CHAT_ROOM, 5L);
    private static final Map<Long, List<Long>> BLOCKED = Map.of(SENDER_ID, List.of(505L));
    private static final Map<Long, List<String>> TOKENS = Map.of(101L, List.of("fcm-101"), 303L, List.of("fcm-303"));

    private FakeNotificationRepository notificationRepository;
    private FakePushSender pushSender;
    private List<List<String>> deletedTokens;
    private NotificationDispatchService dispatchService;

    @BeforeEach
    void setUp() {
        notificationRepository = new FakeNotificationRepository();
        pushSender = new FakePushSender();
        deletedTokens = new ArrayList<>();
        dispatchService = service(memberIds -> memberIds.stream()
                .flatMap(memberId -> TOKENS.getOrDefault(memberId, List.of()).stream())
                .toList());
    }

    @Test
    @DisplayName("교환 요청은 받는 사람 알림 목록에 닉네임 · 제품명으로 저장하고 푸시를 보낸다")
    void exchangeRequested() {
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

        assertEquals(1, pushSender.sent().size());
        FakePushSender.Sent sent = pushSender.sent().getFirst();
        assertEquals(List.of("fcm-101"), sent.pushTokens());
        assertEquals(new PushMessage(NotificationType.EXCHANGE_REQUESTED, "하치와레러버",
                "[포켓몬 카드] 교환 요청이 왔어요", NotificationTargetType.CHAT_ROOM, 5L), sent.message());
    }

    @Test
    @DisplayName("채팅 메시지는 알림 목록에 저장하지 않고 푸시만 보낸다")
    void chatMessageIsPushOnly() {
        // when
        dispatchService.dispatch(request(NotificationType.CHAT_MESSAGE, List.of(101L), SENDER_ID));

        // then
        assertTrue(notificationRepository.findAll().isEmpty());
        assertEquals(1, pushSender.sent().size());
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
    @DisplayName("받는 사람이 여러 명이면 기기 토큰을 모아 한 번에 보낸다")
    void multipleReceivers() {
        // when
        dispatchService.dispatch(request(NotificationType.POST_DELETED, List.of(101L, 303L), SENDER_ID));

        // then
        assertEquals(2, notificationRepository.findAll().size());
        assertEquals(List.of("fcm-101", "fcm-303"), pushSender.sent().getFirst().pushTokens());
    }

    @Test
    @DisplayName("제목에 넣을 상대방(senderId)이 없으면 저장도 푸시도 하지 않는다")
    void withoutSender() {
        // when
        dispatchService.dispatch(request(NotificationType.EXCHANGE_CONFIRMATION, List.of(101L), null));

        // then
        assertTrue(notificationRepository.findAll().isEmpty());
        assertTrue(pushSender.sent().isEmpty());
    }

    @Test
    @DisplayName("기기가 없으면(로그아웃 등) 저장만 하고 푸시는 보내지 않는다")
    void noDevice() {
        // when
        dispatchService.dispatch(request(NotificationType.APPOINTMENT_CANCELED, List.of(404L), SENDER_ID));

        // then
        assertEquals(1, notificationRepository.findAll().size());
        assertTrue(pushSender.sent().isEmpty());
    }

    @Test
    @DisplayName("푸시가 실패해도 알림 목록 저장은 남는다")
    void pushFailureKeepsSaved() {
        // given
        NotificationDispatchService failingPush = service(memberIds -> {
            throw new IllegalStateException("토큰 조회 실패");
        });

        // when
        failingPush.dispatch(request(NotificationType.APPOINTMENT_CANCELED, List.of(101L), SENDER_ID));

        // then
        assertEquals(1, notificationRepository.findAll().size());
    }

    @Test
    @DisplayName("보낸 회원과 차단 관계인 회원에게는 저장도 푸시도 하지 않는다")
    void blockedReceiverIsExcluded() {
        // when
        dispatchService.dispatch(request(NotificationType.CHAT_MESSAGE, List.of(505L), SENDER_ID));
        dispatchService.dispatch(request(NotificationType.POST_UPDATED, List.of(505L, 101L), SENDER_ID));

        // then
        List<Notification> saved = notificationRepository.findAll();
        assertEquals(1, saved.size());
        assertEquals(101L, saved.getFirst().getReceiverId());
        assertEquals(1, pushSender.sent().size());
    }

    @Test
    @DisplayName("약속 취소는 차단 관계여도 보낸다 (차단으로 확정 약속이 취소된 경우)")
    void appointmentCanceledIgnoresBlock() {
        // when
        dispatchService.dispatch(request(NotificationType.APPOINTMENT_CANCELED, List.of(505L), SENDER_ID));

        // then
        assertEquals(1, notificationRepository.findAll().size());
    }

    @Test
    @DisplayName("문장이 없는 종류(출시 후 알림)는 저장도 푸시도 하지 않는다")
    void unsupportedType() {
        // when
        dispatchService.dispatch(request(NotificationType.WISHLIST_KEYWORD, List.of(101L), SENDER_ID));

        // then
        assertTrue(notificationRepository.findAll().isEmpty());
        assertTrue(pushSender.sent().isEmpty());
    }

    @Test
    @DisplayName("FCM이 더 쓸 수 없다고 답한 토큰은 기기 행을 지운다")
    void unusableTokensAreDeleted() {
        // given
        pushSender.markUnusable(List.of("fcm-303"));

        // when
        dispatchService.dispatch(request(NotificationType.EXCHANGE_REQUESTED, List.of(101L, 303L), SENDER_ID));

        // then
        assertEquals(List.of(List.of("fcm-303")), deletedTokens);
    }

    @Test
    @DisplayName("모든 토큰이 정상이면 기기 정리를 부르지 않는다")
    void noUnusableTokens() {
        // when
        dispatchService.dispatch(request(NotificationType.EXCHANGE_REQUESTED, List.of(101L), SENDER_ID));

        // then
        assertTrue(deletedTokens.isEmpty());
    }

    @Test
    @DisplayName("기기 정리가 실패해도 알림 목록 저장은 남는다")
    void cleanupFailureKeepsSaved() {
        // given
        pushSender.markUnusable(List.of("fcm-101"));
        NotificationDispatchService failingCleanup = new NotificationDispatchService(notificationRepository,
                memberId -> "하치와레러버", memberId -> List.of(), memberIds -> List.of("fcm-101"),
                pushTokens -> {
                    throw new IllegalStateException("삭제 실패");
                }, pushSender);

        // when
        failingCleanup.dispatch(request(NotificationType.EXCHANGE_REQUESTED, List.of(101L), SENDER_ID));

        // then
        assertEquals(1, notificationRepository.findAll().size());
    }

    private NotificationDispatchService service(PushTokenPort pushTokenPort) {
        return new NotificationDispatchService(notificationRepository, memberId -> "하치와레러버",
                memberId -> BLOCKED.getOrDefault(memberId, List.of()), pushTokenPort,
                pushTokens -> deletedTokens.add(List.copyOf(pushTokens)), pushSender);
    }

    private static NotificationRequested request(NotificationType type, List<Long> receiverIds, Long senderId) {
        return new NotificationRequested(type, receiverIds, senderId, CHAT_ROOM,
                Map.of(NotificationMessages.PRODUCT_NAME, "포켓몬 카드"), null, null);
    }
}
