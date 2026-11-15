package com.ject.user.infrastructure.social;

import com.ject.user.application.dto.SocialUser;
import com.ject.user.application.exception.InvalidSocialTokenException;
import com.ject.user.application.port.SocialUserClient;
import com.ject.user.domain.domain.SocialProvider;
import org.springframework.stereotype.Component;

@Component
public class SocialUserClientAdapter implements SocialUserClient {

    private final KakaoUserClient kakaoUserClient;
    private final NaverUserClient naverUserClient;

    public SocialUserClientAdapter(KakaoUserClient kakaoUserClient, NaverUserClient naverUserClient) {
        this.kakaoUserClient = kakaoUserClient;
        this.naverUserClient = naverUserClient;
    }

    @Override
    public SocialUser fetch(SocialProvider provider, String accessToken) {
        return switch (provider) {
            case KAKAO -> kakaoUserClient.fetch(accessToken);
            case NAVER -> naverUserClient.fetch(accessToken);
            // Apple은 키를 받은 뒤 별도 이슈에서 구현한다. 그 전에는 로그인 실패(MEMBER_001)로 응답한다
            case APPLE -> throw new InvalidSocialTokenException("Apple 로그인은 아직 지원하지 않습니다.");
        };
    }
}
