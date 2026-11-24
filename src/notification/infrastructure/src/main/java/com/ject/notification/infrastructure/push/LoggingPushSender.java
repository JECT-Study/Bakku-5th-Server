package com.ject.notification.infrastructure.push;

import com.ject.notification.application.dto.PushMessage;
import com.ject.notification.application.port.PushSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 실제로 보내지 않고 로그만 남긴다. FCM 키가 없는 환경(로컬 · 테스트)에서 쓴다.
 * 토큰은 개인 기기 식별값이라 개수만 남긴다.
 */
public class LoggingPushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingPushSender.class);

    @Override
    public List<String> send(List<String> pushTokens, PushMessage message) {
        log.info("푸시 (전송 안 함). type={}, tokens={}, title={}, body={}, target={}:{}", message.type(),
                pushTokens.size(), message.title(), message.body(), message.targetType(), message.targetId());
        return List.of();
    }
}
