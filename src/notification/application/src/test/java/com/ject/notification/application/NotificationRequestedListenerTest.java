package com.ject.notification.application;

import com.ject.common.event.notification.NotificationRequested;
import com.ject.common.event.notification.NotificationType;
import com.ject.notification.application.event.NotificationRequestedListener;
import com.ject.notification.application.service.NotificationDispatchService;
import com.ject.notification.application.service.NotificationScheduleService;
import com.ject.notification.domain.domain.Notification;
import com.ject.notification.domain.domain.ScheduledNotification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationRequestedListenerTest {

    private static final String KEY = "exchange-confirmation:7";
    private static final Instant FIRE_AT = Instant.parse("2026-12-01T06:30:00Z");

    private FakeNotificationRepository notificationRepository;
    private FakeScheduledNotificationRepository scheduledNotificationRepository;
    private NotificationRequestedListener listener;

    @BeforeEach
    void setUp() {
        notificationRepository = new FakeNotificationRepository();
        scheduledNotificationRepository = new FakeScheduledNotificationRepository();
        listener = listener(notificationRepository);
    }

    @Test
    @DisplayName("알림 요청을 받으면 알림 목록에 저장한다")
    void handle() {
        // when
        listener.handle(request(NotificationType.EXCHANGE_CONFIRMATION, 101L, 202L, null, null));

        // then
        assertEquals(1, notificationRepository.findAll().size());
    }

    @Test
    @DisplayName("예약 알림은 바로 보내지 않고 예약해 둔다")
    void scheduled() {
        // when
        listener.handle(request(NotificationType.EXCHANGE_CONFIRMATION, 101L, 202L, FIRE_AT, KEY));

        // then
        assertTrue(notificationRepository.findAll().isEmpty());
        List<ScheduledNotification> scheduled = scheduledNotificationRepository.findAll();
        assertEquals(1, scheduled.size());
        assertEquals(KEY, scheduled.getFirst().getScheduleKey());
        assertEquals(101L, scheduled.getFirst().getReceiverId());
        assertEquals(FIRE_AT, scheduled.getFirst().getFireAt());
    }

    @Test
    @DisplayName("같은 키로 같은 사람에게 다시 예약하면 새 시각으로 바뀐다 (약속 시간 변경)")
    void rescheduleReplaces() {
        // given
        listener.handle(request(NotificationType.EXCHANGE_CONFIRMATION, 101L, 202L, FIRE_AT, KEY));
        Instant changed = FIRE_AT.plusSeconds(3600);

        // when
        listener.handle(request(NotificationType.EXCHANGE_CONFIRMATION, 101L, 202L, changed, KEY));

        // then
        List<ScheduledNotification> scheduled = scheduledNotificationRepository.findAll();
        assertEquals(1, scheduled.size());
        assertEquals(changed, scheduled.getFirst().getFireAt());
    }

    @Test
    @DisplayName("양쪽 알림은 같은 키라도 받는 사람마다 따로 예약된다")
    void bothSidesKeepOwnSchedule() {
        // when
        listener.handle(request(NotificationType.EXCHANGE_CONFIRMATION, 101L, 202L, FIRE_AT, KEY));
        listener.handle(request(NotificationType.EXCHANGE_CONFIRMATION, 202L, 101L, FIRE_AT, KEY));

        // then
        assertEquals(List.of(101L, 202L), scheduledNotificationRepository.findAll().stream()
                .map(ScheduledNotification::getReceiverId)
                .toList());
    }

    @Test
    @DisplayName("바로 보내는 요청에 예약 키가 있으면 그 키의 예약을 모두 취소하고 보낸다 (약속 취소)")
    void immediateWithKeyCancelsSchedule() {
        // given
        listener.handle(request(NotificationType.EXCHANGE_CONFIRMATION, 101L, 202L, FIRE_AT, KEY));
        listener.handle(request(NotificationType.EXCHANGE_CONFIRMATION, 202L, 101L, FIRE_AT, KEY));
        listener.handle(request(NotificationType.EXCHANGE_CONFIRMATION, 101L, 303L, FIRE_AT,
                "exchange-confirmation:8"));

        // when
        listener.handle(request(NotificationType.APPOINTMENT_CANCELED, 101L, 202L, null, KEY));

        // then
        List<ScheduledNotification> scheduled = scheduledNotificationRepository.findAll();
        assertEquals(1, scheduled.size());
        assertEquals("exchange-confirmation:8", scheduled.getFirst().getScheduleKey());
        assertEquals(NotificationType.APPOINTMENT_CANCELED, notificationRepository.findAll().getFirst().getType());
    }

    @Test
    @DisplayName("처리하다 실패해도 예외를 넘기지 않는다 (요청한 쪽은 이미 커밋됨)")
    void failureIsNotPropagated() {
        // given
        NotificationRequestedListener failing = listener(new FakeNotificationRepository() {
            @Override
            public void saveAll(List<Notification> notifications) {
                throw new IllegalStateException("DB 오류");
            }
        });

        // when & then
        assertDoesNotThrow(() -> failing.handle(
                request(NotificationType.EXCHANGE_CONFIRMATION, 101L, 202L, null, null)));
    }

    private NotificationRequestedListener listener(FakeNotificationRepository notificationRepository) {
        return new NotificationRequestedListener(
                new NotificationDispatchService(notificationRepository, memberId -> "하치와레러버",
                        memberId -> List.of(), memberIds -> List.of(), pushTokens -> { }, new FakePushSender()),
                new NotificationScheduleService(scheduledNotificationRepository));
    }

    private static NotificationRequested request(NotificationType type, Long receiverId, Long senderId,
                                                 Instant scheduledAt, String scheduleKey) {
        return new NotificationRequested(type, List.of(receiverId), senderId, null, null, scheduledAt, scheduleKey);
    }
}
