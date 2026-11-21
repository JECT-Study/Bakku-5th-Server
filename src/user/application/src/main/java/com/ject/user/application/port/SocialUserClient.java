package com.ject.user.application.port;

import com.ject.user.application.dto.SocialUser;
import com.ject.user.domain.domain.SocialProvider;

/**
 * 소셜 사용자 정보 조회 출력 포트. FE가 소셜 SDK로 받은 액세스 토큰으로 사용자를 확인한다 (AU-1).
 */
public interface SocialUserClient {

    /**
     * @throws com.ject.user.application.exception.InvalidSocialTokenException 만료 · 위조 · 다른 앱 토큰 · 소셜 서버 오류 (MEMBER_001)
     */
    SocialUser fetch(SocialProvider provider, String accessToken);
}
