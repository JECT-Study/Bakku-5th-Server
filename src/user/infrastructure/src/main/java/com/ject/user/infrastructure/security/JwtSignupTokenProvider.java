package com.ject.user.infrastructure.security;

import com.ject.user.application.dto.SignupIdentity;
import com.ject.user.application.dto.SignupToken;
import com.ject.user.application.exception.InvalidSignupTokenException;
import com.ject.user.application.port.SignupTokenProvider;
import com.ject.user.domain.domain.SocialProvider;
import io.jsonwebtoken.Claims;
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
 * 가입 토큰을 HS256 JWT로 발급 · 검증한다. subject는 소셜 식별값 해시, provider 클레임은 소셜 종류다.
 * 액세스 토큰과 같은 키를 쓰므로 typ 클레임으로 구분한다. 액세스 토큰 검증은 typ이 있으면 거부한다.
 */
@Component
public class JwtSignupTokenProvider implements SignupTokenProvider {

    static final String TYPE_CLAIM = "typ";
    static final String SIGNUP_TYPE = "signup";
    private static final String PROVIDER_CLAIM = "provider";

    private final SecretKey key;
    private final Duration validity;
    private final Clock clock;
    private final JwtParser parser;

    public JwtSignupTokenProvider(@Value("${app.auth.jwt.secret}") String secret,
                                  @Value("${app.auth.jwt.signup-token-validity}") Duration validity,
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
    public SignupToken issue(SignupIdentity identity) {
        Instant issuedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = issuedAt.plus(validity);
        String token = Jwts.builder()
                .subject(identity.socialIdHash())
                .claim(TYPE_CLAIM, SIGNUP_TYPE)
                .claim(PROVIDER_CLAIM, identity.provider().name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
        return new SignupToken(token, expiresAt);
    }

    @Override
    public SignupIdentity verify(String token) {
        try {
            Claims claims = parser.parseSignedClaims(token).getPayload();
            String provider = claims.get(PROVIDER_CLAIM, String.class);
            if (!SIGNUP_TYPE.equals(claims.get(TYPE_CLAIM, String.class)) || claims.getSubject() == null
                    || provider == null) {
                throw new InvalidSignupTokenException();
            }
            return new SignupIdentity(SocialProvider.valueOf(provider), claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            // 만료 · 서명 불일치 · 형식 오류 · provider 값 오류는 모두 다시 로그인하게 한다
            throw new InvalidSignupTokenException();
        }
    }
}
