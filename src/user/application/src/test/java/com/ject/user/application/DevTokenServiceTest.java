package com.ject.user.application;

import com.ject.common.core.time.ServiceTime;
import com.ject.user.application.dto.DevToken;
import com.ject.user.application.service.DevTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevTokenServiceTest {

    private DevTokenService devTokenService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-12T03:00:00Z"), ZoneId.of("Asia/Seoul"));
        devTokenService = new DevTokenService(new FakeMemberRepository(), new FakeAccessTokenProvider(),
                new ServiceTime(clock));
    }

    @Test
    @DisplayName("처음 보는 닉네임이면 테스트 회원을 만들어 토큰을 준다")
    void createMember() {
        // when
        DevToken token = devTokenService.issue("테스트요청자");

        // then
        assertEquals("테스트요청자", token.nickname());
        assertEquals("token-" + token.memberId(), token.accessToken().value());
    }

    @Test
    @DisplayName("같은 닉네임(대소문자 무시)으로 다시 부르면 같은 회원으로 발급한다")
    void reuseMember() {
        // given
        Long memberId = devTokenService.issue("TesterA").memberId();

        // when
        DevToken again = devTokenService.issue("testera");

        // then
        assertEquals(memberId, again.memberId());
    }

    @Test
    @DisplayName("닉네임을 비우면 '테스트+숫자' 닉네임으로 매번 새 회원을 만든다")
    void randomNickname() {
        // when
        DevToken first = devTokenService.issue(null);
        DevToken second = devTokenService.issue(" ");

        // then
        assertTrue(first.nickname().matches("테스트\\d{6}"));
        assertNotEquals(first.memberId(), second.memberId());
    }
}
