package com.ject.user.application.port;

import java.util.Optional;

/**
 * Apple 토큰 교환 · 취소 출력 포트 (Sign in with Apple REST API).
 * 탈퇴 때 Apple 토큰을 취소하려면 로그인 때 받은 authorization code를 리프레시 토큰으로 바꿔 둬야 한다 (AU-1).
 */
public interface AppleTokenClient {

    /**
     * @return Apple 리프레시 토큰. 키가 설정되지 않아 교환하지 않으면 빈 값
     */
    Optional<String> exchange(String authorizationCode);

    /**
     * 키가 설정되지 않으면 아무것도 하지 않는다.
     */
    void revoke(String refreshToken);
}
