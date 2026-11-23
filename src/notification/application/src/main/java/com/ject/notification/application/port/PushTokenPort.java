package com.ject.notification.application.port;

import java.util.Collection;
import java.util.List;

/**
 * 받는 사람들의 기기 토큰 조회 (회원 BC). 로그아웃 · 탈퇴한 기기는 나오지 않는다.
 */
public interface PushTokenPort {

    List<String> getPushTokens(Collection<Long> memberIds);
}
