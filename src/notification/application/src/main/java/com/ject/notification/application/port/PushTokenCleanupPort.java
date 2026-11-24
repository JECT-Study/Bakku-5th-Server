package com.ject.notification.application.port;

import java.util.Collection;

/**
 * 더 쓸 수 없는 기기 토큰 정리 (회원 BC).
 */
public interface PushTokenCleanupPort {

    void deletePushTokens(Collection<String> pushTokens);
}
