package com.ject.user.application;

import com.ject.user.application.port.AppleTokenClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * authorization code "code-x"를 리프레시 토큰 "refresh-x"로 바꾼다. "fail"이 들어간 값은 Apple 오류처럼 실패한다.
 */
class FakeAppleTokenClient implements AppleTokenClient {

    final List<String> revoked = new ArrayList<>();

    @Override
    public Optional<String> exchange(String authorizationCode) {
        if (authorizationCode.contains("fail")) {
            throw new IllegalStateException("Apple 토큰 교환 실패");
        }
        return Optional.of(authorizationCode.replace("code-", "refresh-"));
    }

    @Override
    public void revoke(String refreshToken) {
        if (refreshToken.contains("fail")) {
            throw new IllegalStateException("Apple 토큰 취소 실패");
        }
        revoked.add(refreshToken);
    }
}
