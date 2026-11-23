package com.ject.notification.application.scheduler;

import com.ject.notification.application.service.ScheduledNotificationSendService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 1분마다 보낼 시각이 지난 예약 알림을 보낸다. 발송 오차는 최대 1분 (D-02-1, 결정 C4).
 */
@Component
public class ScheduledNotificationJob {

    private static final Logger log = LoggerFactory.getLogger(ScheduledNotificationJob.class);

    private final ScheduledNotificationSendService scheduledNotificationSendService;

    public ScheduledNotificationJob(ScheduledNotificationSendService scheduledNotificationSendService) {
        this.scheduledNotificationSendService = scheduledNotificationSendService;
    }

    /**
     * 한 번에 다 못 잡았으면(BATCH_SIZE만큼 잡음) 남은 예약을 이어서 보낸다.
     */
    @Scheduled(fixedDelayString = "PT1M")
    public void run() {
        try {
            while (scheduledNotificationSendService.sendDue() == ScheduledNotificationSendService.BATCH_SIZE) {
                // 남은 예약이 없을 때까지 반복
            }
        } catch (RuntimeException e) {
            log.error("예약 알림 배치 실패", e);
        }
    }
}
