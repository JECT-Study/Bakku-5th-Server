package com.ject.user.application.query;

import java.util.Collection;
import java.util.List;

/**
 * 알림 BC가 푸시를 보낼 기기 토큰을 조회하는 계약.
 * 로그아웃 · 탈퇴하면 기기 행을 지우므로 그 기기 토큰은 나오지 않는다 (G-09-1 · G-09-2).
 */
public interface PushTokenQuery {

    /**
     * 회원들의 기기 토큰 전부. 기기가 없는 회원은 빠진다.
     */
    List<String> getPushTokens(Collection<Long> memberIds);
}
