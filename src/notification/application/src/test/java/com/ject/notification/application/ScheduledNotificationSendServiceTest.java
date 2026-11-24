package com.ject.notification.application;

import com.ject.common.core.time.ServiceTime;
import com.ject.common.event.notification.NotificationRequested;
import com.ject.common.event.notification.NotificationType;
import com.ject.notification.application.service.NotificationDispatchService;
import com.ject.notification.application.service.ScheduledNotificationSendService;
import com.ject.notification.domain.domain.Notification;
import com.ject.notification.domain.domain.ScheduledNotification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScheduledNotificationSendServiceTest {

    private static final Instant NOW = Instant.parse("2026-12-01T06:30:00Z");

    private FakeNotificationRepository notificationRepository;
    private FakeScheduledNotificationRepository scheduledNotificationRepository;
    private ScheduledNotificationSendService sendService;

    @BeforeEach
    void setUp() {
        notificationRepository = new FakeNotificationRepository();
        scheduledNotificationRepository = new FakeScheduledNotificationRepository();
        sendService = service(notificationRepository);
    }

    @Test
    @DisplayName("보낼 시각이 지난 예약만 보내고 지운다")
    void sendDue() {
        // given
        schedule(101L, NOW.minusSeconds(30));
        schedule(303L, NOW);
        schedule(404L, NOW.plusSeconds(60));

        // when
        int sent = sendService.sendDue();

        // then
        assertEquals(2, sent);
        assertEquals(List.of(101L, 303L), notificationRepository.findAll().stream()
                .map(Notification::getReceiverId)
                .toList());
        List<ScheduledNotification> left = scheduledNotificationRepository.findAll();
        assertEquals(1, left.size());
        assertEquals(404L, left.getFirst().getReceiverId());
    }

    @Test
    @DisplayName("예약할 때 받은 닉네임 · 제품명 · 이동 대상으로 보낸다")
    void sendWithScheduledContent() {
        // given
        schedule(101L, NOW);

        // when
        sendService.sendDue();

        // then
        Notification notification = notificationRepository.findAll().getFirst();
        assertEquals(NotificationType.EXCHANGE_CONFIRMATION, notification.getType());
        assertEquals("하치와레러버", notification.getTitle());
        assertTrue(notification.getBody().startsWith("[포켓몬 카드] "));
    }

    @Test
    @DisplayName("한 번에 BATCH_SIZE만큼만 잡는다")
    void limitedByBatchSize() {
        // given
        IntStream.range(0, ScheduledNotificationSendService.BATCH_SIZE + 1)
                .forEach(i -> schedule(1000L + i, NOW));

        // when
        int sent = sendService.sendDue();

        // then
        assertEquals(ScheduledNotificationSendService.BATCH_SIZE, sent);
        assertEquals(1, scheduledNotificationRepository.findAll().size());
    }

    @Test
    @DisplayName("한 건이 실패해도 나머지는 보내고, 실패한 건도 다시 보내지 않게 지운다")
    void failureDoesNotStopOthers() {
        // given
        ScheduledNotificationSendService failingFor101 = service(new FakeNotificationRepository() {
            @Override
            public void saveAll(List<Notification> notifications) {
                if (notifications.getFirst().getReceiverId().equals(101L)) {
                    throw new IllegalStateException("DB 오류");
                }
                super.saveAll(notifications);
            }
        });
        schedule(101L, NOW);
        schedule(303L, NOW);

        // when
        int sent = failingFor101.sendDue();

        // then
        assertEquals(2, sent);
        assertTrue(scheduledNotificationRepository.findAll().isEmpty());
    }

    private ScheduledNotificationSendService service(FakeNotificationRepository notificationRepository) {
        NotificationDispatchService dispatchService = new NotificationDispatchService(notificationRepository,
                memberId -> "하치와레러버", memberId -> List.of(), memberIds -> List.of(), pushTokens -> { },
                new FakePushSender());
        return new ScheduledNotificationSendService(scheduledNotificationRepository, dispatchService,
                new ServiceTime(Clock.fixed(NOW, ZoneId.of("Asia/Seoul"))));
    }

    private void schedule(Long receiverId, Instant fireAt) {
        scheduledNotificationRepository.saveAll(ScheduledNotification.from(new NotificationRequested(
                NotificationType.EXCHANGE_CONFIRMATION, List.of(receiverId), 202L, null,
                Map.of("productName", "포켓몬 카드"), fireAt, "exchange-confirmation:" + receiverId)));
    }
}
