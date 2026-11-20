package com.ject.user.infrastructure.social;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ject.user.application.dto.SocialUser;
import com.ject.user.application.exception.InvalidSocialTokenException;
import com.ject.user.domain.domain.Gender;
import com.ject.user.domain.domain.SocialProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.DateTimeException;
import java.time.LocalDate;

/**
 * 네이버 회원 프로필 API로 회원번호와 가입 화면 자동 채우기 값(생년월일 · 성별)을 받는다 (결정 A18).
 */
@Component
public class NaverUserClient {

    private static final Logger log = LoggerFactory.getLogger(NaverUserClient.class);
    private static final String PROFILE_URL = "https://openapi.naver.com/v1/nid/me";
    private static final String SUCCESS = "00";

    private final RestClient restClient;

    public NaverUserClient(RestClient socialRestClient) {
        this.restClient = socialRestClient;
    }

    public SocialUser fetch(String accessToken) {
        ProfileResponse profile = request(accessToken);
        if (profile == null || !SUCCESS.equals(profile.resultCode()) || profile.response() == null
                || profile.response().id() == null) {
            throw new InvalidSocialTokenException("네이버 회원번호를 받지 못했습니다.");
        }
        Profile user = profile.response();
        return new SocialUser(SocialProvider.NAVER, user.id(), birthDateOf(user), genderOf(user.gender()));
    }

    private ProfileResponse request(String accessToken) {
        try {
            return restClient.get()
                    .uri(PROFILE_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(ProfileResponse.class);
        } catch (HttpClientErrorException e) {
            throw new InvalidSocialTokenException("네이버 토큰이 만료됐거나 올바르지 않습니다.");
        } catch (RestClientException e) {
            log.error("네이버 프로필 조회 실패", e);
            throw new InvalidSocialTokenException("네이버 서버에 연결하지 못했습니다.");
        }
    }

    /**
     * 출생연도(YYYY)와 생일(MM-DD)을 모두 받았을 때만 만든다. 사용자가 동의하지 않으면 비어 있다.
     */
    private static LocalDate birthDateOf(Profile user) {
        if (user.birthyear() == null || user.birthday() == null) {
            return null;
        }
        try {
            return LocalDate.parse(user.birthyear() + "-" + user.birthday());
        } catch (DateTimeException e) {
            return null;
        }
    }

    /**
     * F · M만 채우고, U(확인 불가)나 미동의는 null (명세 AU-1 prefill.gender).
     */
    private static Gender genderOf(String gender) {
        if ("F".equals(gender)) {
            return Gender.FEMALE;
        }
        if ("M".equals(gender)) {
            return Gender.MALE;
        }
        return null;
    }

    record ProfileResponse(@JsonProperty("resultcode") String resultCode, Profile response) {
    }

    record Profile(String id, String gender, String birthday, String birthyear) {
    }
}
