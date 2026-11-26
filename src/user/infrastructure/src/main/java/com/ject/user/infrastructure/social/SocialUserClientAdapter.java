package com.ject.user.infrastructure.social;

import com.ject.user.application.dto.SocialUser;
import com.ject.user.application.port.SocialUserClient;
import com.ject.user.domain.domain.SocialProvider;
import org.springframework.stereotype.Component;

@Component
public class SocialUserClientAdapter implements SocialUserClient {

    private final KakaoUserClient kakaoUserClient;
    private final NaverUserClient naverUserClient;
    private final AppleUserClient appleUserClient;

    public SocialUserClientAdapter(KakaoUserClient kakaoUserClient, NaverUserClient naverUserClient,
                                   AppleUserClient appleUserClient) {
        this.kakaoUserClient = kakaoUserClient;
        this.naverUserClient = naverUserClient;
        this.appleUserClient = appleUserClient;
    }

    @Override
    public SocialUser fetch(SocialProvider provider, String accessToken) {
        return switch (provider) {
            case KAKAO -> kakaoUserClient.fetch(accessToken);
            case NAVER -> naverUserClient.fetch(accessToken);
            case APPLE -> appleUserClient.fetch(accessToken);
        };
    }
}
