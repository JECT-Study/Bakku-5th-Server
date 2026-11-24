package com.ject.notification.application.port;

import com.ject.notification.application.dto.PushMessage;

import java.util.List;

/**
 * 기기로 푸시를 보내는 출력 포트. 키가 있으면 FCM, 없으면 로그만 남기는 구현을 쓴다.
 */
public interface PushSender {

    void send(List<String> pushTokens, PushMessage message);
}
