package com.ject.user.application;

import com.ject.common.core.exception.CommonErrorCode;
import com.ject.common.core.time.ServiceTime;
import com.ject.user.application.dto.LoginTokens;
import com.ject.user.application.exception.InvalidRefreshTokenException;
import com.ject.user.application.service.LoginTokenIssuer;
import com.ject.user.application.service.TokenRefreshService;
import com.ject.user.domain.domain.BirthDate;
import com.ject.user.domain.domain.Member;
import com.ject.user.domain.domain.MemberStatus;
import com.ject.user.domain.domain.Nickname;
import com.ject.user.domain.domain.RefreshToken;
import com.ject.user.domain.domain.SocialProvider;
import com.ject.user.domain.exception.MemberBannedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenRefreshServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-18T03:00:00Z");
    private static final String DEVICE_ID = "device-1";

    private FakeMemberRepository memberRepository;
    private FakeRefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        memberRepository = new FakeMemberRepository();
        refreshTokenRepository = new FakeRefreshTokenRepository();
    }

    @Test
    @DisplayName("현재 토큰이면 새 액세스 · 리프레시 토큰을 주고, 받은 토큰은 직전 토큰이 된다")
    void refresh_currentToken_rotates() {
        // given
        Member member = save(MemberStatus.ACTIVE);
        String issued = login(member);

        // when
        LoginTokens tokens = service(NOW).refresh(issued);

        // then
        assertEquals("token-" + member.getId(), tokens.accessToken().value());
        assertEquals(member.getId(), tokens.memberId());
        assertNotEquals(issued, tokens.refreshToken());
        RefreshToken stored = refreshTokenRepository.findByDeviceId(DEVICE_ID).orElseThrow();
        assertEquals(LoginTokenIssuer.hash(tokens.refreshToken()), stored.getTokenHash());
        assertEquals(LoginTokenIssuer.hash(issued), stored.getPreviousTokenHash());
        assertEquals(NOW, stored.getRotatedAt());
    }

    @Test
    @DisplayName("직전 토큰이 회전 후 10초 안에 오면 AUTH_005이고 토큰은 그대로 둔다")
    void refresh_previousTokenWithinWindow_alreadyReissued() {
        // given
        Member member = save(MemberStatus.ACTIVE);
        String issued = login(member);
        LoginTokens rotated = service(NOW).refresh(issued);

        // when
        InvalidRefreshTokenException e = assertThrows(InvalidRefreshTokenException.class,
                () -> service(NOW.plusSeconds(10)).refresh(issued));

        // then
        assertEquals(CommonErrorCode.TOKEN_ALREADY_REISSUED, e.getErrorCode());
        assertEquals(LoginTokenIssuer.hash(rotated.refreshToken()),
                refreshTokenRepository.findByDeviceId(DEVICE_ID).orElseThrow().getTokenHash());
    }

    @Test
    @DisplayName("직전 토큰이 회전 후 10초가 지나 오면 재사용으로 보고 기기 토큰을 지운 뒤 AUTH_004")
    void refresh_previousTokenAfterWindow_deletesDeviceToken() {
        // given
        Member member = save(MemberStatus.ACTIVE);
        String issued = login(member);
        LoginTokens rotated = service(NOW).refresh(issued);

        // when
        InvalidRefreshTokenException e = assertThrows(InvalidRefreshTokenException.class,
                () -> service(NOW.plusSeconds(11)).refresh(issued));

        // then
        assertEquals(CommonErrorCode.REFRESH_TOKEN_INVALID, e.getErrorCode());
        assertEquals(0, refreshTokenRepository.count());
        assertThrows(InvalidRefreshTokenException.class,
                () -> service(NOW.plusSeconds(12)).refresh(rotated.refreshToken()));
    }

    @Test
    @DisplayName("저장된 적 없는 토큰이면 AUTH_004")
    void refresh_unknownToken_invalid() {
        // when
        InvalidRefreshTokenException e = assertThrows(InvalidRefreshTokenException.class,
                () -> service(NOW).refresh("unknown-token"));

        // then
        assertEquals(CommonErrorCode.REFRESH_TOKEN_INVALID, e.getErrorCode());
    }

    @Test
    @DisplayName("다른 요청이 먼저 회전해 저장하지 못하면 AUTH_005")
    void refresh_lostRace_alreadyReissued() {
        // given
        Member member = save(MemberStatus.ACTIVE);
        String issued = login(member);
        RefreshToken loaded = refreshTokenRepository.findByDeviceId(DEVICE_ID).orElseThrow();
        service(NOW).refresh(issued);

        // when: 먼저 읽어 둔 토큰으로 회전하면 저장된 현재 토큰이 이미 바뀌어 있다
        InvalidRefreshTokenException e = assertThrows(InvalidRefreshTokenException.class,
                () -> issuer(NOW).rotate(member, loaded));

        // then
        assertEquals(CommonErrorCode.TOKEN_ALREADY_REISSUED, e.getErrorCode());
    }

    @Test
    @DisplayName("영구 정지 회원이면 MEMBER_003")
    void refresh_bannedMember() {
        // given
        Member member = save(MemberStatus.BANNED);
        String issued = login(member);

        // when & then
        assertThrows(MemberBannedException.class, () -> service(NOW).refresh(issued));
    }

    @Test
    @DisplayName("탈퇴한 회원의 토큰이면 지우고 AUTH_004")
    void refresh_withdrawnMember_deletesToken() {
        // given
        Member member = save(MemberStatus.ACTIVE);
        String issued = login(member);
        member.withdraw(NOW);
        memberRepository.save(member);

        // when
        InvalidRefreshTokenException e = assertThrows(InvalidRefreshTokenException.class,
                () -> service(NOW).refresh(issued));

        // then
        assertEquals(CommonErrorCode.REFRESH_TOKEN_INVALID, e.getErrorCode());
        assertTrue(refreshTokenRepository.findByDeviceId(DEVICE_ID).isEmpty());
    }

    private TokenRefreshService service(Instant now) {
        return new TokenRefreshService(refreshTokenRepository, memberRepository, issuer(now), serviceTime(now));
    }

    private LoginTokenIssuer issuer(Instant now) {
        return new LoginTokenIssuer(new FakeAccessTokenProvider(), refreshTokenRepository,
                new FakePushDeviceRepository(), serviceTime(now));
    }

    private String login(Member member) {
        return issuer(NOW.minusSeconds(3600)).issue(member, DEVICE_ID).refreshToken();
    }

    private Member save(MemberStatus status) {
        Member member = Member.restore(null, SocialProvider.KAKAO, "hash-1", new Nickname("쿠로미덕후"),
                new BirthDate(LocalDate.of(2000, 1, 1)), null, status, null, 0, false, NOW, null, null, NOW, null);
        return memberRepository.save(member);
    }

    private static ServiceTime serviceTime(Instant now) {
        return new ServiceTime(Clock.fixed(now, ZoneId.of("Asia/Seoul")));
    }
}
