package com.ject.user.application.service;

import com.ject.common.core.time.ServiceTime;
import com.ject.user.application.dto.LoginTokens;
import com.ject.user.application.exception.InvalidRefreshTokenException;
import com.ject.user.application.port.MemberRepository;
import com.ject.user.application.port.RefreshTokenRepository;
import com.ject.user.domain.domain.Member;
import com.ject.user.domain.domain.RefreshToken;
import com.ject.user.domain.exception.MemberBannedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 리프레시 토큰으로 토큰을 재발급한다 (AU-3). 리프레시 토큰은 시간으로 만료되지 않고 쓸 때마다 회전한다 (결정 A1 · C2 · C3).
 */
@Service
public class TokenRefreshService {

    private static final Logger log = LoggerFactory.getLogger(TokenRefreshService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberRepository memberRepository;
    private final LoginTokenIssuer loginTokenIssuer;
    private final ServiceTime serviceTime;

    public TokenRefreshService(RefreshTokenRepository refreshTokenRepository, MemberRepository memberRepository,
                               LoginTokenIssuer loginTokenIssuer, ServiceTime serviceTime) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.memberRepository = memberRepository;
        this.loginTokenIssuer = loginTokenIssuer;
        this.serviceTime = serviceTime;
    }

    /**
     * 현재 토큰이면 새 토큰으로 회전한다. 직전 토큰이 회전 후 10초 안에 오면 동시 요청으로 보고(AUTH_005),
     * 10초가 지나 오면 탈취로 보고 그 기기의 토큰을 지운다(AUTH_004). 지운 것은 예외를 던져도 커밋한다.
     *
     * @throws InvalidRefreshTokenException 무효 (AUTH_004) · 이미 재발급됨 (AUTH_005)
     * @throws MemberBannedException        영구 정지 회원 (MEMBER_003)
     */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public LoginTokens refresh(String refreshToken) {
        String tokenHash = LoginTokenIssuer.hash(refreshToken);
        Optional<RefreshToken> current = refreshTokenRepository.findByTokenHash(tokenHash);
        if (current.isPresent()) {
            return rotate(current.get());
        }

        RefreshToken rotated = refreshTokenRepository.findByPreviousTokenHash(tokenHash)
                .orElseThrow(InvalidRefreshTokenException::invalid);
        if (rotated.isConcurrentRefresh(serviceTime.now())) {
            throw InvalidRefreshTokenException.alreadyReissued();
        }
        refreshTokenRepository.delete(rotated);
        log.warn("회전한 리프레시 토큰이 10초 뒤 다시 와서 기기 토큰을 지웠습니다. memberId={}, deviceId={}",
                rotated.getMemberId(), rotated.getDeviceId());
        throw InvalidRefreshTokenException.invalid();
    }

    /**
     * 탈퇴했거나 없는 회원의 토큰은 지우고 무효로 본다. 영구 정지는 FE가 정지 안내를 띄우도록 MEMBER_003을 준다 (A-09-1).
     */
    private LoginTokens rotate(RefreshToken stored) {
        Optional<Member> member = memberRepository.findById(stored.getMemberId())
                .filter(found -> !found.isWithdrawn());
        if (member.isEmpty()) {
            refreshTokenRepository.delete(stored);
            throw InvalidRefreshTokenException.invalid();
        }
        if (member.get().isBanned()) {
            throw new MemberBannedException();
        }
        return loginTokenIssuer.rotate(member.get(), stored);
    }
}
