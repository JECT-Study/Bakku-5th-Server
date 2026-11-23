package com.ject.notification.application.service;

import com.ject.common.core.time.ServiceTime;
import com.ject.notification.application.dto.NotificationPage;
import com.ject.notification.application.dto.NotificationPage.NotificationItem;
import com.ject.notification.application.port.NotificationRepository;
import com.ject.notification.domain.domain.Notification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 알림 목록 조회. 30일 지난 알림은 삭제 배치 전이라도 보이지 않는다 (X-01-1).
 */
@Service
@Transactional(readOnly = true)
public class NotificationQueryService {

    private final NotificationRepository notificationRepository;
    private final ServiceTime serviceTime;

    public NotificationQueryService(NotificationRepository notificationRepository, ServiceTime serviceTime) {
        this.notificationRepository = notificationRepository;
        this.serviceTime = serviceTime;
    }

    /**
     * NT-1 최신순 목록. 한 건 더 가져와 다음 페이지가 있는지 본다.
     *
     * @param cursorId 이전 페이지 마지막 알림 id, 첫 페이지면 null
     */
    public NotificationPage getNotifications(Long memberId, Long cursorId, int size) {
        List<Notification> rows = notificationRepository.findPage(memberId,
                Notification.visibleFrom(serviceTime.now()), cursorId, size + 1);
        boolean hasNext = rows.size() > size;
        List<Notification> page = hasNext ? rows.subList(0, size) : rows;
        Long nextCursorId = hasNext ? page.getLast().getId() : null;
        return new NotificationPage(page.stream().map(NotificationQueryService::toItem).toList(), nextCursorId,
                hasNext);
    }

    private static NotificationItem toItem(Notification notification) {
        return new NotificationItem(notification.getId(), notification.getType(), notification.getTitle(),
                notification.getBody(), notification.getTargetType(), notification.getTargetId(),
                notification.isRead(), notification.getCreatedAt());
    }
}
