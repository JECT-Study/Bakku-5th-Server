package com.ject.user.infrastructure.security;

import com.ject.user.application.dto.AccessToken;
import com.ject.user.application.exception.InvalidAccessTokenException;
import com.ject.user.application.port.AccessTokenProvider;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * 액세스 토큰을 HS256 JWT로 발급 · 검증한다. subject는 회원 id다.
 * 서명 키는 환경변수 JWT_SECRET(32바이트 이상)이며, 짧으면 앱이 뜨지 않는다.
 */
@Component
public class JwtAccessTokenProvider implements AccessTokenProvider {

    private final SecretKey key;
    private final Duration validity;
    private final Clock clock;
    private final JwtParser parser;

    public JwtAccessTokenProvider(@Value("${app.auth.jwt.secret}") String secret,
                                  @Value("${app.auth.jwt.access-token-validity}") Duration validity,
                                  Clock clock) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.validity = validity;
        this.clock = clock;
        this.parser = Jwts.parser()
                .verifyWith(key)
                .clock(() -> Date.from(clock.instant()))
                .build();
    }

    @Override
    public AccessToken issue(Long memberId) {
        // JWT 시각은 초 단위라, 응답의 만료 시각도 토큰과 같게 초 단위로 맞춘다
        Instant issuedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = issuedAt.plus(validity);
        String token = Jwts.builder()
                .subject(String.valueOf(memberId))
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
        return new AccessToken(token, expiresAt);
    }

    @Override
    public Long verify(String token) {
        try {
            String subject = parser.parseSignedClaims(token).getPayload().getSubject();
            return Long.valueOf(subject);
        } catch (ExpiredJwtException e) {
            throw InvalidAccessTokenException.expired();
        } catch (JwtException | IllegalArgumentException e) {
            // 서명 불일치 · 형식 오류 · subject가 숫자가 아님 (NumberFormatException도 IllegalArgumentException)
            throw InvalidAccessTokenException.invalid();
        }
    }
}
