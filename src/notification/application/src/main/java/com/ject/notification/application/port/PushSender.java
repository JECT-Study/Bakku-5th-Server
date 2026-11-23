package com.ject.notification.application.port;

import com.ject.notification.application.dto.PushMessage;

import java.util.List;

/**
 * 기기로 푸시를 보내는 출력 포트. FCM 연동은 키를 받은 뒤 붙인다.
 */
public interface PushSender {

    void send(List<String> pushTokens, PushMessage message);
}
