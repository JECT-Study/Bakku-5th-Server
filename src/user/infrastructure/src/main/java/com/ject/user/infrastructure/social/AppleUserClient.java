package com.ject.user.infrastructure.social;

import com.ject.user.application.dto.SocialUser;
import com.ject.user.application.exception.InvalidSocialTokenException;
import com.ject.user.domain.domain.SocialProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.LocatorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Apple identity token(JWT)을 Apple 공개키로 검증해 Apple 사용자 ID(sub)를 얻는다 (A-02-1).
 * 서명 · 발급자 · 대상(번들 ID) · 만료를 확인한다. Apple은 생년월일 · 성별을 주지 않는다 (결정 A18).
 */
@Component
public class AppleUserClient {

    static final String KEYS_URL = "https://appleid.apple.com/auth/keys";
    static final String ISSUER = "https://appleid.apple.com";

    private static final Logger log = LoggerFactory.getLogger(AppleUserClient.class);

    /** Apple은 공개키를 가끔 바꾼다. 하루마다 새로 받고, 모르는 키 ID가 오면 바로 다시 받는다 */
    private static final Duration KEYS_MAX_AGE = Duration.ofDays(1);
    /** 위조 토큰이 모르는 키 ID를 계속 보내도 Apple 서버를 매번 부르지 않게 한다 */
    private static final Duration MIN_REFRESH_INTERVAL = Duration.ofMinutes(1);
    /** 우리 서버와 Apple 서버의 시계 차이를 만료 · 발급 시각 확인에서 이만큼 봐준다 */
    private static final Duration CLOCK_SKEW = Duration.ofSeconds(60);

    private final RestClient restClient;
    private final String clientId;
    private final Clock clock;

    private Map<String, PublicKey> keys = Map.of();
    private Instant keysFetchedAt;

    /**
     * @param clientId identity token의 대상(aud). 네이티브 앱이라 번들 ID다
     */
    public AppleUserClient(RestClient socialRestClient, @Value("${app.auth.apple.client-id}") String clientId,
                           Clock clock) {
        this.restClient = socialRestClient;
        this.clientId = clientId;
        this.clock = clock;
    }

    public SocialUser fetch(String identityToken) {
        Claims claims;
        try {
            claims = Jwts.parser()
                    .keyLocator(new AppleKeyLocator())
                    .requireIssuer(ISSUER)
                    .requireAudience(clientId)
                    .clock(() -> Date.from(clock.instant()))
                    .clockSkewSeconds(CLOCK_SKEW.toSeconds())
                    .build()
                    .parseSignedClaims(identityToken)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            // 자세한 이유(공개키 값 등)는 응답에 싣지 않고 로그로만 남긴다
            log.warn("Apple identity token 검증 실패: {}", e.getMessage());
            throw new InvalidSocialTokenException("Apple identity token을 검증하지 못했습니다.");
        }
        String appleUserId = claims.getSubject();
        if (appleUserId == null || appleUserId.isBlank()) {
            throw new InvalidSocialTokenException("Apple 사용자 ID가 없습니다.");
        }
        return new SocialUser(SocialProvider.APPLE, appleUserId, null, null);
    }

    private synchronized PublicKey findKey(String keyId) {
        Instant now = clock.instant();
        boolean stale = keysFetchedAt == null || now.isAfter(keysFetchedAt.plus(KEYS_MAX_AGE));
        boolean unknown = !keys.containsKey(keyId);
        boolean canRefresh = keysFetchedAt == null || now.isAfter(keysFetchedAt.plus(MIN_REFRESH_INTERVAL));
        if (stale || (unknown && canRefresh)) {
            keys = requestKeys();
            keysFetchedAt = now;
        }
        PublicKey key = keys.get(keyId);
        if (key == null) {
            throw new InvalidSocialTokenException("Apple 공개키 목록에 없는 키입니다. kid=" + keyId);
        }
        return key;
    }

    private Map<String, PublicKey> requestKeys() {
        AppleKeys response;
        try {
            response = restClient.get().uri(KEYS_URL).retrieve().body(AppleKeys.class);
        } catch (RestClientException e) {
            log.error("Apple 공개키 조회 실패", e);
            throw new InvalidSocialTokenException("Apple 서버에 연결하지 못했습니다.");
        }
        if (response == null || response.keys() == null) {
            throw new InvalidSocialTokenException("Apple 공개키를 받지 못했습니다.");
        }
        Map<String, PublicKey> result = new HashMap<>();
        for (AppleKey key : response.keys()) {
            if ("RSA".equals(key.kty())) {
                result.put(key.kid(), toPublicKey(key));
            }
        }
        return Map.copyOf(result);
    }

    private static PublicKey toPublicKey(AppleKey key) {
        try {
            BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(key.n()));
            BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(key.e()));
            return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(modulus, exponent));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new InvalidSocialTokenException("Apple 공개키를 읽지 못했습니다. kid=" + key.kid());
        }
    }

    private class AppleKeyLocator extends LocatorAdapter<Key> {

        @Override
        protected Key locate(JwsHeader header) {
            return findKey(header.getKeyId());
        }
    }

    record AppleKeys(List<AppleKey> keys) {
    }

    /**
     * @param n RSA modulus (Base64 URL)
     * @param e RSA exponent (Base64 URL)
     */
    record AppleKey(String kid, String kty, String alg, String n, String e) {
    }
}
