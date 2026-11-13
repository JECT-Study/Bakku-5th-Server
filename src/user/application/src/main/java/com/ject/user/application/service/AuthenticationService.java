package com.ject.user.application.service;

import com.ject.user.application.port.AccessTokenProvider;
import com.ject.user.application.port.MemberRepository;
import com.ject.user.domain.domain.Member;
import com.ject.user.domain.exception.MemberBannedException;
import com.ject.user.domain.exception.MemberNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 요청마다 액세스 토큰을 확인하고 회원 상태를 본다. 정지 · 탈퇴가 바로 반영되도록 캐시 없이 매번 조회한다.
 */
@Service
@Transactional(readOnly = true)
public class AuthenticationService {

    private final AccessTokenProvider accessTokenProvider;
    private final MemberRepository memberRepository;

    public AuthenticationService(AccessTokenProvider accessTokenProvider, MemberRepository memberRepository) {
        this.accessTokenProvider = accessTokenProvider;
        this.memberRepository = memberRepository;
    }

    /**
     * @return 로그인한 회원 id
     * @throws com.ject.user.application.exception.InvalidAccessTokenException 토큰 만료 · 위조 (AUTH_002 · AUTH_003)
     * @throws MemberNotFoundException 없는 회원 · 탈퇴한 회원 (MEMBER_201, MB-2와 같음)
     * @throws MemberBannedException   영구 정지 회원 (MEMBER_003)
     */
    public Long authenticate(String accessToken) {
        Long memberId = accessTokenProvider.verify(accessToken);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));
        if (member.isWithdrawn()) {
            throw new MemberNotFoundException(memberId);
        }
        if (member.isBanned()) {
            throw new MemberBannedException();
        }
        return memberId;
    }
}
