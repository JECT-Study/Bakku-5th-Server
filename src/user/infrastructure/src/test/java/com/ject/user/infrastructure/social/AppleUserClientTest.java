package com.ject.user.infrastructure.social;

import com.ject.user.application.dto.SocialUser;
import com.ject.user.application.exception.InvalidSocialTokenException;
import com.ject.user.domain.domain.SocialProvider;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 테스트용으로 새로 만든 RSA 키로 Apple 공개키 목록 · identity token을 흉내 낸다 (실제 Apple 키 아님).
 */
class AppleUserClientTest {

    private static final String CLIENT_ID = "com.ject.bakku";
    private static final Instant NOW = Instant.parse("2026-11-25T12:00:00Z");

    private static KeyPair appleKey;
    private static KeyPair otherKey;

    private RestClient.Builder builder;
    private MockRestServiceServer server;

    @BeforeAll
    static void generateKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        appleKey = generator.generateKeyPair();
        otherKey = generator.generateKeyPair();
    }

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
    }

    @Test
    @DisplayName("Apple 공개키로 서명 · 발급자 · 대상 · 만료가 맞으면 Apple 사용자 ID를 받고, 생년월일 · 성별은 비어 있다")
    void fetch() {
        // given
        respondKeys("kid-1");

        // when
        SocialUser user = client(NOW).fetch(token("kid-1", appleKey, AppleUserClient.ISSUER, CLIENT_ID, NOW));

        // then
        assertEquals(SocialProvider.APPLE, user.provider());
        assertEquals("001234.apple-user.0001", user.socialId());
        assertNull(user.birthDate());
        assertNull(user.gender());
        server.verify();
    }

    @Test
    @DisplayName("다른 앱(번들 ID)에서 받은 토큰이면 MEMBER_001")
    void otherAudience() {
        respondKeys("kid-1");
        assertInvalid(client(NOW), token("kid-1", appleKey, AppleUserClient.ISSUER, "com.other.app", NOW));
    }

    @Test
    @DisplayName("Apple이 발급하지 않은 토큰(발급자 다름)이면 MEMBER_001")
    void otherIssuer() {
        respondKeys("kid-1");
        assertInvalid(client(NOW), token("kid-1", appleKey, "https://evil.example.com", CLIENT_ID, NOW));
    }

    @Test
    @DisplayName("만료된 토큰이면 MEMBER_001")
    void expired() {
        respondKeys("kid-1");
        String token = token("kid-1", appleKey, AppleUserClient.ISSUER, CLIENT_ID, NOW.minus(Duration.ofHours(2)));
        assertInvalid(client(NOW), token);
    }

    @Test
    @DisplayName("Apple 키가 아닌 키로 서명한 토큰(위조)이면 MEMBER_001")
    void forgedSignature() {
        respondKeys("kid-1");
        assertInvalid(client(NOW), token("kid-1", otherKey, AppleUserClient.ISSUER, CLIENT_ID, NOW));
    }

    @Test
    @DisplayName("JWT 형식이 아니면 MEMBER_001")
    void notJwt() {
        assertInvalid(client(NOW), "not-a-jwt");
    }

    @Test
    @DisplayName("Apple 서버 오류면 MEMBER_001")
    void appleServerError() {
        server.expect(requestTo(AppleUserClient.KEYS_URL)).andRespond(withServerError());
        assertInvalid(client(NOW), token("kid-1", appleKey, AppleUserClient.ISSUER, CLIENT_ID, NOW));
    }

    @Test
    @DisplayName("공개키는 한 번 받아 두고 다시 쓴다")
    void keysAreCached() {
        // given
        respondKeys("kid-1");
        AppleUserClient client = client(NOW);

        // when
        client.fetch(token("kid-1", appleKey, AppleUserClient.ISSUER, CLIENT_ID, NOW));
        client.fetch(token("kid-1", appleKey, AppleUserClient.ISSUER, CLIENT_ID, NOW));

        // then: 공개키 요청은 한 번뿐
        server.verify();
    }

    @Test
    @DisplayName("Apple이 키를 바꿔 모르는 키 ID가 오면 공개키를 다시 받는다")
    void refreshOnUnknownKeyId() {
        // given: 처음엔 kid-1만, 1분 뒤 다시 받을 때는 kid-2도 있음
        server.expect(ExpectedCount.once(), requestTo(AppleUserClient.KEYS_URL))
                .andRespond(withSuccess(keysJson("kid-1"), MediaType.APPLICATION_JSON));
        server.expect(ExpectedCount.once(), requestTo(AppleUserClient.KEYS_URL))
                .andRespond(withSuccess(keysJson("kid-1", "kid-2"), MediaType.APPLICATION_JSON));
        MutableClock clock = new MutableClock(NOW);
        AppleUserClient client = new AppleUserClient(builder.build(), CLIENT_ID, clock);
        client.fetch(token("kid-1", appleKey, AppleUserClient.ISSUER, CLIENT_ID, NOW));
        clock.instant = NOW.plus(Duration.ofMinutes(2));

        // when
        SocialUser user = client.fetch(token("kid-2", appleKey, AppleUserClient.ISSUER, CLIENT_ID, clock.instant));

        // then
        assertEquals("001234.apple-user.0001", user.socialId());
        server.verify();
    }

    private void assertInvalid(AppleUserClient client, String token) {
        assertThrows(InvalidSocialTokenException.class, () -> client.fetch(token));
    }

    private AppleUserClient client(Instant now) {
        return new AppleUserClient(builder.build(), CLIENT_ID, Clock.fixed(now, ZoneId.of("UTC")));
    }

    private void respondKeys(String... keyIds) {
        server.expect(ExpectedCount.once(), requestTo(AppleUserClient.KEYS_URL))
                .andRespond(withSuccess(keysJson(keyIds), MediaType.APPLICATION_JSON));
    }

    /**
     * Apple 공개키 목록 형식. 테스트에서는 모든 키 ID가 같은 테스트용 키를 가리킨다.
     */
    private static String keysJson(String... keyIds) {
        RSAPublicKey publicKey = (RSAPublicKey) appleKey.getPublic();
        String n = base64Url(publicKey.getModulus());
        String e = base64Url(publicKey.getPublicExponent());
        String keyFormat = "{\"kty\":\"RSA\",\"kid\":\"%s\",\"use\":\"sig\",\"alg\":\"RS256\","
                + "\"n\":\"%s\",\"e\":\"%s\"}";
        String keys = String.join(",", Arrays.stream(keyIds)
                .map(kid -> keyFormat.formatted(kid, n, e))
                .toList());
        return "{\"keys\":[" + keys + "]}";
    }

    private static String token(String keyId, KeyPair signer, String issuer, String audience, Instant issuedAt) {
        return Jwts.builder()
                .header().keyId(keyId).and()
                .issuer(issuer)
                .audience().add(audience).and()
                .subject("001234.apple-user.0001")
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plus(Duration.ofMinutes(10))))
                .signWith(signer.getPrivate(), Jwts.SIG.RS256)
                .compact();
    }

    private static String base64Url(BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes[0] == 0) {
            bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static class MutableClock extends Clock {

        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
