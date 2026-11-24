package com.ject.notification.application.service;

import com.ject.common.core.time.ServiceTime;
import com.ject.notification.application.port.NotificationRepository;
import com.ject.notification.domain.domain.Notification;
import com.ject.notification.domain.exception.NotificationNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 알림 읽음 처리. 30일 지난 알림은 삭제 배치 전이라도 없는 알림으로 본다 (X-01-1).
 */
@Service
@Transactional
public class NotificationReadService {

    private final NotificationRepository notificationRepository;
    private final ServiceTime serviceTime;

    public NotificationReadService(NotificationRepository notificationRepository, ServiceTime serviceTime) {
        this.notificationRepository = notificationRepository;
        this.serviceTime = serviceTime;
    }

    /**
     * NT-3 이미 읽은 알림이어도 성공으로 본다.
     *
     * @throws NotificationNotFoundException 알림이 없거나 남의 알림 · 30일 지난 알림
     */
    public void read(Long memberId, Long notificationId) {
        Instant now = serviceTime.now();
        Instant visibleFrom = Notification.visibleFrom(now);
        if (notificationRepository.markRead(notificationId, memberId, now, visibleFrom)) {
            return;
        }
        if (!notificationRepository.exists(notificationId, memberId, visibleFrom)) {
            throw new NotificationNotFoundException(notificationId);
        }
    }

    /**
     * NT-4 안 읽은 알림(30일 안)을 모두 읽음으로 바꾼다.
     *
     * @return 이번에 읽음으로 바꾼 수
     */
    public long readAll(Long memberId) {
        Instant now = serviceTime.now();
        return notificationRepository.markAllRead(memberId, now, Notification.visibleFrom(now));
    }
}
