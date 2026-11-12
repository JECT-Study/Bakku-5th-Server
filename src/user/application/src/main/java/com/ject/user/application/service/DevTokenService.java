package com.ject.user.application.service;

import com.ject.common.core.time.ServiceTime;
import com.ject.user.application.dto.DevToken;
import com.ject.user.application.port.AccessTokenProvider;
import com.ject.user.application.port.MemberRepository;
import com.ject.user.domain.domain.BirthDate;
import com.ject.user.domain.domain.Member;
import com.ject.user.domain.domain.Nickname;
import com.ject.user.domain.domain.SocialProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 소셜 로그인 없이 테스트 회원의 액세스 토큰을 발급한다. local · dev 프로필에서만 등록된다.
 * 같은 닉네임으로 다시 부르면 그 회원으로 다시 발급해서, 여러 명이 필요한 흐름(요청자 · 게시자)도 시험할 수 있다.
 */
@Service
@Profile({"local", "dev"})
@Transactional
public class DevTokenService {

    private static final BirthDate TEST_BIRTH_DATE = new BirthDate(LocalDate.of(2000, 1, 1));
    private static final String TEST_SOCIAL_ID_PREFIX = "dev-";
    private static final int MAX_NICKNAME_ATTEMPTS = 5;

    private final MemberRepository memberRepository;
    private final AccessTokenProvider accessTokenProvider;
    private final ServiceTime serviceTime;

    public DevTokenService(MemberRepository memberRepository, AccessTokenProvider accessTokenProvider,
                           ServiceTime serviceTime) {
        this.memberRepository = memberRepository;
        this.accessTokenProvider = accessTokenProvider;
        this.serviceTime = serviceTime;
    }

    /**
     * @param nickname 비우면 "테스트" + 숫자로 새 회원을 만든다. 이미 있는 닉네임이면 그 회원으로 발급한다
     */
    public DevToken issue(String nickname) {
        Member member = nickname == null || nickname.isBlank()
                ? create(newRandomNickname())
                : findOrCreate(new Nickname(nickname));
        return new DevToken(member.getId(), member.getNickname().value(), accessTokenProvider.issue(member.getId()));
    }

    private Member findOrCreate(Nickname nickname) {
        return memberRepository.findByNickname(nickname).orElseGet(() -> create(nickname));
    }

    private Member create(Nickname nickname) {
        Member member = Member.create(SocialProvider.KAKAO, TEST_SOCIAL_ID_PREFIX + UUID.randomUUID(), nickname,
                TEST_BIRTH_DATE, null, false, null, serviceTime.now(), serviceTime.today());
        return memberRepository.save(member);
    }

    private Nickname newRandomNickname() {
        for (int attempt = 0; attempt < MAX_NICKNAME_ATTEMPTS; attempt++) {
            Nickname candidate = new Nickname("테스트" + ThreadLocalRandom.current().nextInt(100_000, 1_000_000));
            if (memberRepository.findByNickname(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new IllegalStateException("테스트 닉네임을 만들지 못했습니다.");
    }
}
