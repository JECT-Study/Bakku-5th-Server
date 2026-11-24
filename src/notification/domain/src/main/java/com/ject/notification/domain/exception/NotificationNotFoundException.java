package com.ject.notification.domain.exception;

import com.ject.common.core.exception.DomainException;

public class NotificationNotFoundException extends DomainException {

    public NotificationNotFoundException(Long notificationId) {
        super(NotificationErrorCode.NOTIFICATION_NOT_FOUND, "알림을 찾을 수 없습니다. id=" + notificationId);
    }
}
