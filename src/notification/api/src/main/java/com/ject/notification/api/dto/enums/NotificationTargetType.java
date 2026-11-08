package com.ject.notification.api.dto.enums;

/**
 * 알림을 누르면 이동할 대상. 1차는 CHAT_ROOM만 사용.
 */
public enum NotificationTargetType {
    CHAT_ROOM,
    POST,
    SEARCH,
    REVIEW,
    MY_POSTS,
    MY_PROFILE
}
