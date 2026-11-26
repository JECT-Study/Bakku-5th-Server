package com.ject.user.application.port;

import java.util.Optional;

/**
 * Apple 리프레시 토큰 저장소. 가입 전에도 받아 두므로 회원 id가 아니라 소셜 식별값 해시로 찾는다.
 * 구현은 토큰을 암호화해 저장한다 (AU-1).
 */
public interface AppleTokenRepository {

    /**
     * 같은 소셜 계정이면 새 토큰으로 바꾼다.
     */
    void save(String socialIdHash, String refreshToken);

    Optional<String> find(String socialIdHash);

    void delete(String socialIdHash);
}
