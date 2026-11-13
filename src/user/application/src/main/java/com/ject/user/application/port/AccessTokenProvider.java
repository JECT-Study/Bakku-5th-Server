package com.ject.user.application.port;

import com.ject.user.application.dto.AccessToken;

/**
 * 액세스 토큰 발급 · 검증 출력 포트. 서명 방식(JWT)은 infrastructure가 담당한다.
 */
public interface AccessTokenProvider {

    AccessToken issue(Long memberId);

    /**
     * @return 토큰에 담긴 회원 id
     * @throws com.ject.user.application.exception.InvalidAccessTokenException 만료(AUTH_002) · 위조(AUTH_003)
     */
    Long verify(String token);
}
