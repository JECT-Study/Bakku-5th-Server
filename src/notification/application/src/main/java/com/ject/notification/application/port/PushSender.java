package com.ject.notification.application.port;

import com.ject.notification.application.dto.PushMessage;

import java.util.List;

/**
 * 기기로 푸시를 보내는 출력 포트. 키가 있으면 FCM, 없으면 로그만 남기는 구현을 쓴다.
 */
public interface PushSender {

    /**
     * @return 앱 삭제 등으로 더 쓸 수 없다고 확인된 토큰, 없으면 빈 목록
     */
    List<String> send(List<String> pushTokens, PushMessage message);
}
