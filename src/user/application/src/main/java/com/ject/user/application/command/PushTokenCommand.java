package com.ject.user.application.command;

import java.util.Collection;

/**
 * 알림 BC가 더 쓸 수 없는 기기 토큰을 지우는 계약.
 */
public interface PushTokenCommand {

    /**
     * FCM이 더 쓸 수 없다고 알려 준 토큰(앱 삭제 등)의 기기 행을 지운다. 없는 토큰은 무시한다.
     */
    void deletePushTokens(Collection<String> pushTokens);
}
