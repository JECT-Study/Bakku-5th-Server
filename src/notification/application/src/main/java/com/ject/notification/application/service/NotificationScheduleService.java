package com.ject.notification.application.service;

import com.ject.common.event.notification.NotificationRequested;
import com.ject.notification.application.port.ScheduledNotificationRepository;
import com.ject.notification.domain.domain.ScheduledNotification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 예약 알림을 받아 두고, 같은 키로 다시 오면 바꾸고, 키로 취소한다 (결정 C4).
 */
@Service
public class NotificationScheduleService {

    private final ScheduledNotificationRepository scheduledNotificationRepository;

    public NotificationScheduleService(ScheduledNotificationRepository scheduledNotificationRepository) {
        this.scheduledNotificationRepository = scheduledNotificationRepository;
    }

    /**
     * 요청한 쪽 트랜잭션이 커밋된 뒤 불리므로 새 트랜잭션을 연다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void schedule(NotificationRequested request) {
        scheduledNotificationRepository.saveAll(ScheduledNotification.from(request));
    }

    /**
     * 그 키로 걸린 예약을 받는 사람과 상관없이 모두 지운다. 없으면 아무것도 하지 않는다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cancel(String scheduleKey) {
        scheduledNotificationRepository.deleteByScheduleKey(scheduleKey);
    }
}
