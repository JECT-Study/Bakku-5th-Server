package com.ject.user.infrastructure.social;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 테스트용으로 새로 만든 EC 키로 .p8 키를 흉내 낸다 (실제 Apple 키 아님).
 */
class AppleTokenClientAdapterTest {

    private static final String CLIENT_ID = "com.ject.bakku";
    private static final String TEAM_ID = "TEAMID0001";
    private static final String KEY_ID = "KEYID00001";
    private static final Instant NOW = Instant.parse("2026-11-26T03:00:00Z");

    private static KeyPair p8Key;
    private static String encodedP8;

    private RestClient.Builder builder;
    private MockRestServiceServer server;

    @BeforeAll
    static void generateKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        p8Key = generator.generateKeyPair();
        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                .encodeToString(p8Key.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";
        encodedP8 = Base64.getEncoder().encodeToString(pem.getBytes(StandardCharsets.UTF_8));
    }

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
    }

    @Test
    @DisplayName("authorization code를 Apple에 보내 리프레시 토큰을 받는다")
    void exchange() {
        // given
        AtomicReference<Map<String, String>> form = new AtomicReference<>();
        server.expect(requestTo(AppleTokenClientAdapter.TOKEN_URL))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(request -> form.set(parseForm(request.getBody().toString())))
                .andRespond(withSuccess("{\"access_token\":\"a\",\"token_type\":\"Bearer\",\"expires_in\":3600,"
                        + "\"refresh_token\":\"r.apple-refresh\",\"id_token\":\"i\"}", MediaType.APPLICATION_JSON));

        // when
        Optional<String> refreshToken = client(encodedP8).exchange("apple-code");

        // then
        assertEquals(Optional.of("r.apple-refresh"), refreshToken);
        assertEquals(CLIENT_ID, form.get().get("client_id"));
        assertEquals("apple-code", form.get().get("code"));
        assertEquals("authorization_code", form.get().get("grant_type"));
        assertClientSecret(form.get().get("client_secret"));
        server.verify();
    }

    @Test
    @DisplayName("Apple이 거절하면(만료 · 이미 쓴 code) 실패한다")
    void exchangeRejected() {
        server.expect(requestTo(AppleTokenClientAdapter.TOKEN_URL))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"invalid_grant\"}"));

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> client(encodedP8).exchange("used-code"));
        assertTrue(e.getMessage().contains("invalid_grant"));
    }

    @Test
    @DisplayName("리프레시 토큰을 Apple에 보내 취소한다")
    void revoke() {
        // given
        AtomicReference<Map<String, String>> form = new AtomicReference<>();
        server.expect(requestTo(AppleTokenClientAdapter.REVOKE_URL))
                .andExpect(request -> form.set(parseForm(request.getBody().toString())))
                .andRespond(withSuccess());

        // when
        client(encodedP8).revoke("r.apple-refresh");

        // then
        assertEquals("r.apple-refresh", form.get().get("token"));
        assertEquals("refresh_token", form.get().get("token_type_hint"));
        assertEquals(CLIENT_ID, form.get().get("client_id"));
        assertClientSecret(form.get().get("client_secret"));
        server.verify();
    }

    @Test
    @DisplayName("Apple이 취소를 거절하면 실패한다 (보관한 토큰을 지우지 않게)")
    void revokeRejected() {
        server.expect(requestTo(AppleTokenClientAdapter.REVOKE_URL))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThrows(IllegalStateException.class, () -> client(encodedP8).revoke("r.apple-refresh"));
    }

    @Test
    @DisplayName("키가 없으면 교환은 하지 않고, 취소는 실패로 본다 (Apple을 부르지 않음)")
    void disabled() {
        AppleTokenClientAdapter client = client("");

        assertEquals(Optional.empty(), client.exchange("apple-code"));
        assertThrows(IllegalStateException.class, () -> client.revoke("r.apple-refresh"));
        server.verify();
    }

    @Test
    @DisplayName(".p8 키 형식이 틀리면 원인을 알려 주며 실패한다")
    void invalidPrivateKey() {
        String notP8 = Base64.getEncoder().encodeToString("not a key".getBytes(StandardCharsets.UTF_8));

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> client(notP8));
        assertTrue(e.getMessage().contains("APPLE_PRIVATE_KEY"));
    }

    /**
     * Apple이 요구하는 client secret: ES256, 헤더 kid = Key ID, iss = Team ID, sub = Client ID, aud = Apple.
     */
    private static void assertClientSecret(String clientSecret) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(p8Key.getPublic())
                .clock(() -> Date.from(NOW))
                .build()
                .parseSignedClaims(clientSecret);
        assertEquals("ES256", jws.getHeader().getAlgorithm());
        assertEquals(KEY_ID, jws.getHeader().getKeyId());
        assertEquals(TEAM_ID, jws.getPayload().getIssuer());
        assertEquals(CLIENT_ID, jws.getPayload().getSubject());
        assertTrue(jws.getPayload().getAudience().contains(AppleTokenClientAdapter.AUDIENCE));
        assertTrue(jws.getPayload().getExpiration().toInstant().isAfter(NOW));
    }

    private static Map<String, String> parseForm(String body) {
        return Arrays.stream(body.split("&"))
                .map(pair -> pair.split("=", 2))
                .collect(Collectors.toMap(pair -> decode(pair[0]), pair -> pair.length > 1 ? decode(pair[1]) : ""));
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private AppleTokenClientAdapter client(String encodedPrivateKey) {
        String teamId = encodedPrivateKey.isBlank() ? "" : TEAM_ID;
        String keyId = encodedPrivateKey.isBlank() ? "" : KEY_ID;
        return new AppleTokenClientAdapter(builder.build(), CLIENT_ID, teamId, keyId, encodedPrivateKey,
                Clock.fixed(NOW, ZoneId.of("UTC")));
    }
}
