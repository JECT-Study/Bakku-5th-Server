package com.ject.notification.infrastructure.config;

import com.ject.common.event.notification.NotificationRequested;
import com.ject.common.event.notification.NotificationType;
import com.ject.notification.application.event.NotificationAsyncDispatcher;
import com.ject.notification.application.event.NotificationRequestedListener;
import com.ject.notification.application.service.NotificationDispatchService;
import com.ject.notification.application.service.NotificationScheduleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.event.TransactionalEventListenerFactory;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 실제 스프링 설정으로 알림 요청이 어느 스레드에서 처리되는지 확인한다.
 */
class NotificationAsyncConfigTest {

    private static final String KEY = "exchange-confirmation:7";

    private AnnotationConfigApplicationContext context;
    private RecordingDispatchService dispatchService;
    private RecordingScheduleService scheduleService;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(TestConfig.class);
        dispatchService = context.getBean(RecordingDispatchService.class);
        scheduleService = context.getBean(RecordingScheduleService.class);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    @DisplayName("바로 보내는 알림은 알림 전용 스레드(notification-)에서 처리하고, 발행한 쪽은 기다리지 않는다")
    void dispatchRunsOnNotificationThread() throws InterruptedException {
        // given
        dispatchService.block();

        // when
        context.publishEvent(request(null, null));

        // then
        assertEquals(List.of(), dispatchService.threads, "발행한 스레드가 처리를 기다리지 않아야 한다");
        dispatchService.release();
        assertTrue(dispatchService.done.await(5, TimeUnit.SECONDS));
        assertTrue(dispatchService.threads.getFirst().startsWith("notification-"));
    }

    @Test
    @DisplayName("예약 · 취소는 발행한 스레드에서 발행한 순서대로 바로 처리한다")
    void scheduleAndCancelRunInOrderOnCallerThread() {
        // given
        String caller = Thread.currentThread().getName();

        // when
        context.publishEvent(request(KEY, Instant.parse("2026-12-01T06:30:00Z")));
        context.publishEvent(request(KEY, null));
        context.publishEvent(request(KEY, Instant.parse("2026-12-01T07:30:00Z")));

        // then
        assertEquals(List.of("schedule", "cancel", "schedule"), scheduleService.calls);
        assertEquals(List.of(caller, caller, caller), scheduleService.threads);
    }

    @Test
    @DisplayName("알림 처리가 실패해도 발행한 쪽으로 예외가 넘어가지 않는다")
    void failureDoesNotReachPublisher() throws InterruptedException {
        // given
        dispatchService.failNext();

        // when
        context.publishEvent(request(null, null));

        // then
        assertTrue(dispatchService.done.await(5, TimeUnit.SECONDS));
    }

    private static NotificationRequested request(String scheduleKey, Instant scheduledAt) {
        return new NotificationRequested(NotificationType.APPOINTMENT_CANCELED, List.of(101L), 202L, null, null,
                scheduledAt, scheduleKey);
    }

    @Configuration
    @Import({NotificationAsyncConfig.class, NotificationRequestedListener.class, NotificationAsyncDispatcher.class})
    static class TestConfig {

        @Bean
        TransactionalEventListenerFactory transactionalEventListenerFactory() {
            return new TransactionalEventListenerFactory();
        }

        @Bean
        RecordingDispatchService dispatchService() {
            return new RecordingDispatchService();
        }

        @Bean
        RecordingScheduleService scheduleService() {
            return new RecordingScheduleService();
        }
    }

    static class RecordingDispatchService extends NotificationDispatchService {

        final List<String> threads = new CopyOnWriteArrayList<>();
        final CountDownLatch done = new CountDownLatch(1);
        private CountDownLatch gate = new CountDownLatch(0);
        private volatile boolean fail;

        RecordingDispatchService() {
            super(null, null, null, null, null, null);
        }

        void block() {
            gate = new CountDownLatch(1);
        }

        void release() {
            gate.countDown();
        }

        void failNext() {
            fail = true;
        }

        @Override
        public void dispatch(NotificationRequested request) {
            try {
                gate.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            threads.add(Thread.currentThread().getName());
            done.countDown();
            if (fail) {
                throw new IllegalStateException("FCM 오류");
            }
        }
    }

    static class RecordingScheduleService extends NotificationScheduleService {

        final List<String> calls = new CopyOnWriteArrayList<>();
        final List<String> threads = new CopyOnWriteArrayList<>();

        RecordingScheduleService() {
            super(null);
        }

        @Override
        public void schedule(NotificationRequested request) {
            calls.add("schedule");
            threads.add(Thread.currentThread().getName());
        }

        @Override
        public void cancel(String scheduleKey) {
            calls.add("cancel");
            threads.add(Thread.currentThread().getName());
        }
    }
}
