package com.ject.notification.domain.domain;

import java.util.Objects;

/**
 * 푸시와 알림 목록에 보여 줄 두 줄. 제목은 상대 닉네임, 내용은 종류별 문장과 제품명이다 (X-01-1).
 */
public record NotificationContent(String title, String body) {

    public NotificationContent {
        Objects.requireNonNull(title);
        Objects.requireNonNull(body);
    }
}
