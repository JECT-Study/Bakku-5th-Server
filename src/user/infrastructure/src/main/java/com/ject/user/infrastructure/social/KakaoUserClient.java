package com.ject.user.infrastructure.social;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ject.user.application.dto.SocialUser;
import com.ject.user.application.exception.InvalidSocialTokenException;
import com.ject.user.domain.domain.SocialProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 카카오 액세스 토큰 정보 API로 회원번호와 발급 앱을 확인한다.
 * 다른 앱에서 받은 토큰으로 로그인하지 못하도록 앱 ID를 비교한다 (명세 AU-1 "카카오는 앱 ID도 확인").
 * 카카오는 생년월일 · 성별을 받지 않는다 (결정 A18).
 */
@Component
public class KakaoUserClient {

    private static final Logger log = LoggerFactory.getLogger(KakaoUserClient.class);
    private static final String TOKEN_INFO_URL = "https://kapi.kakao.com/v1/user/access_token_info";

    private final RestClient restClient;
    private final String appId;

    public KakaoUserClient(RestClient socialRestClient, @Value("${app.auth.kakao.app-id:}") String appId) {
        this.restClient = socialRestClient;
        this.appId = appId;
        if (appId.isBlank()) {
            log.warn("카카오 앱 ID(KAKAO_APP_ID)가 없어 토큰의 발급 앱을 확인하지 않습니다.");
        }
    }

    public SocialUser fetch(String accessToken) {
        TokenInfo info = request(accessToken);
        if (info == null || info.id() == null) {
            throw new InvalidSocialTokenException("카카오 회원번호를 받지 못했습니다.");
        }
        if (!appId.isBlank() && !appId.equals(String.valueOf(info.appId()))) {
            throw new InvalidSocialTokenException("다른 앱에서 발급된 카카오 토큰입니다.");
        }
        return new SocialUser(SocialProvider.KAKAO, String.valueOf(info.id()), null, null);
    }

    private TokenInfo request(String accessToken) {
        try {
            return restClient.get()
                    .uri(TOKEN_INFO_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(TokenInfo.class);
        } catch (HttpClientErrorException e) {
            throw new InvalidSocialTokenException("카카오 토큰이 만료됐거나 올바르지 않습니다.");
        } catch (RestClientException e) {
            log.error("카카오 토큰 정보 조회 실패", e);
            throw new InvalidSocialTokenException("카카오 서버에 연결하지 못했습니다.");
        }
    }

    record TokenInfo(Long id, @JsonProperty("app_id") Long appId) {
    }
}
