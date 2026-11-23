package com.ject.notification.application.service;

import com.ject.common.core.time.ServiceTime;
import com.ject.notification.application.port.ScheduledNotificationRepository;
import com.ject.notification.domain.domain.ScheduledNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 보낼 시각이 지난 예약 알림을 보내고 지운다 (결정 C4).
 */
@Service
public class ScheduledNotificationSendService {

    /** 한 번에 잡는 예약 수. 더 남아 있으면 배치가 이어서 부른다 */
    public static final int BATCH_SIZE = 100;

    private static final Logger log = LoggerFactory.getLogger(ScheduledNotificationSendService.class);

    private final ScheduledNotificationRepository scheduledNotificationRepository;
    private final NotificationDispatchService notificationDispatchService;
    private final ServiceTime serviceTime;

    public ScheduledNotificationSendService(ScheduledNotificationRepository scheduledNotificationRepository,
                                            NotificationDispatchService notificationDispatchService,
                                            ServiceTime serviceTime) {
        this.scheduledNotificationRepository = scheduledNotificationRepository;
        this.notificationDispatchService = notificationDispatchService;
        this.serviceTime = serviceTime;
    }

    /**
     * 잡은 예약은 이 트랜잭션이 끝날 때까지 잠겨 있어 다른 서버가 같은 예약을 또 보내지 않는다.
     * 한 건이 실패해도 나머지는 보내고, 실패한 건도 다시 보내지 않도록 지운다 (매분 같은 실패 반복 방지).
     *
     * @return 이번에 잡은 예약 수
     */
    @Transactional
    public int sendDue() {
        List<ScheduledNotification> due = scheduledNotificationRepository.findDueForUpdate(serviceTime.now(),
                BATCH_SIZE);
        if (due.isEmpty()) {
            return 0;
        }
        due.forEach(this::send);
        scheduledNotificationRepository.deleteAllById(due.stream().map(ScheduledNotification::getId).toList());
        return due.size();
    }

    private void send(ScheduledNotification scheduled) {
        try {
            notificationDispatchService.dispatch(scheduled.toRequest());
        } catch (RuntimeException e) {
            log.error("예약 알림 발송 실패. id={}, type={}, receiverId={}, scheduleKey={}", scheduled.getId(),
                    scheduled.getType(), scheduled.getReceiverId(), scheduled.getScheduleKey(), e);
        }
    }
}
