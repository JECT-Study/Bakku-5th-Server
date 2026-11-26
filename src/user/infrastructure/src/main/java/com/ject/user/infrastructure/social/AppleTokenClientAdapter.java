package com.ject.user.infrastructure.social;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ject.user.application.port.AppleTokenClient;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

/**
 * Sign in with Apple REST API로 authorization code를 리프레시 토큰으로 바꾸고, 탈퇴 때 토큰을 취소한다.
 * 요청마다 Team ID · Key ID · .p8 키로 서명한 client secret(ES256 JWT)을 보낸다.
 * 키가 없으면(로컬 · 설정 전) 교환하지 않고, 취소는 실패로 본다 (보관한 토큰을 지우지 않게).
 */
@Component
public class AppleTokenClientAdapter implements AppleTokenClient {

    static final String TOKEN_URL = "https://appleid.apple.com/auth/token";
    static final String REVOKE_URL = "https://appleid.apple.com/auth/revoke";
    static final String AUDIENCE = "https://appleid.apple.com";

    private static final Logger log = LoggerFactory.getLogger(AppleTokenClientAdapter.class);

    /** client secret은 요청할 때마다 새로 만들어 짧게 쓴다 (Apple 최대 6개월) */
    private static final Duration CLIENT_SECRET_VALIDITY = Duration.ofMinutes(5);

    private final RestClient restClient;
    private final String clientId;
    private final String teamId;
    private final String keyId;
    private final PrivateKey privateKey;
    private final Clock clock;

    /**
     * @param encodedPrivateKey .p8 키 파일 내용을 Base64로 인코딩한 값 (환경변수 APPLE_PRIVATE_KEY)
     */
    public AppleTokenClientAdapter(RestClient socialRestClient,
                                   @Value("${app.auth.apple.client-id}") String clientId,
                                   @Value("${app.auth.apple.team-id:}") String teamId,
                                   @Value("${app.auth.apple.key-id:}") String keyId,
                                   @Value("${app.auth.apple.private-key:}") String encodedPrivateKey,
                                   Clock clock) {
        this.restClient = socialRestClient;
        this.clientId = clientId;
        this.teamId = teamId;
        this.keyId = keyId;
        this.clock = clock;
        if (teamId.isBlank() || keyId.isBlank() || encodedPrivateKey.isBlank()) {
            log.warn("Apple 키(APPLE_TEAM_ID · APPLE_KEY_ID · APPLE_PRIVATE_KEY)가 없어 Apple 토큰 교환 · 취소를 하지 않습니다.");
            this.privateKey = null;
        } else {
            this.privateKey = toPrivateKey(encodedPrivateKey);
        }
    }

    @Override
    public Optional<String> exchange(String authorizationCode) {
        if (privateKey == null) {
            return Optional.empty();
        }
        MultiValueMap<String, String> form = baseForm();
        form.add("code", authorizationCode);
        form.add("grant_type", "authorization_code");
        TokenResponse response;
        try {
            response = restClient.post().uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("Apple 토큰 교환 실패. status=" + e.getStatusCode()
                    + ", body=" + e.getResponseBodyAsString(), e);
        }
        if (response == null || response.refreshToken() == null) {
            throw new IllegalStateException("Apple 토큰 교환 응답에 리프레시 토큰이 없습니다.");
        }
        return Optional.of(response.refreshToken());
    }

    @Override
    public void revoke(String refreshToken) {
        if (privateKey == null) {
            throw new IllegalStateException("Apple 키가 없어 Apple 토큰을 취소하지 못했습니다.");
        }
        MultiValueMap<String, String> form = baseForm();
        form.add("token", refreshToken);
        form.add("token_type_hint", "refresh_token");
        try {
            restClient.post().uri(REVOKE_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw new IllegalStateException("Apple 토큰 취소 실패. status=" + e.getStatusCode()
                    + ", body=" + e.getResponseBodyAsString(), e);
        }
    }

    private MultiValueMap<String, String> baseForm() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret());
        return form;
    }

    /**
     * iss = Team ID, sub = Client ID(번들 ID), aud = Apple, 헤더 kid = Key ID, ES256 서명.
     */
    String clientSecret() {
        Instant now = clock.instant();
        return Jwts.builder()
                .header().keyId(keyId).and()
                .issuer(teamId)
                .subject(clientId)
                .audience().add(AUDIENCE).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(CLIENT_SECRET_VALIDITY)))
                .signWith(privateKey, Jwts.SIG.ES256)
                .compact();
    }

    /**
     * .p8 파일은 "-----BEGIN PRIVATE KEY-----"로 감싼 PKCS#8 EC 키다.
     */
    private static PrivateKey toPrivateKey(String encodedPrivateKey) {
        try {
            String pem = new String(Base64.getDecoder().decode(encodedPrivateKey.trim()), StandardCharsets.UTF_8);
            String body = pem.replaceAll("-----(BEGIN|END) PRIVATE KEY-----", "").replaceAll("\\s", "");
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(body));
            return KeyFactory.getInstance("EC").generatePrivate(spec);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("APPLE_PRIVATE_KEY는 .p8 파일 내용을 Base64로 인코딩한 값이어야 합니다.", e);
        }
    }

    record TokenResponse(@JsonProperty("refresh_token") String refreshToken) {
    }
}
