package com.ject.user.application.event;

import com.ject.common.event.member.MemberWithdrawn;
import com.ject.user.application.port.MemberRepository;
import com.ject.user.application.service.AppleTokenService;
import com.ject.user.domain.domain.SocialProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Apple로 가입한 회원이 탈퇴하면 Apple 토큰을 취소한다 (G-09-2, 애플 심사 지침 5.1.1(v)).
 * 탈퇴는 이미 커밋돼서, 실패해도 예외를 넘기지 않고 로그로 남긴다.
 */
@Component
public class AppleTokenRevokeListener {

    private static final Logger log = LoggerFactory.getLogger(AppleTokenRevokeListener.class);

    private final MemberRepository memberRepository;
    private final AppleTokenService appleTokenService;

    public AppleTokenRevokeListener(MemberRepository memberRepository, AppleTokenService appleTokenService) {
        this.memberRepository = memberRepository;
        this.appleTokenService = appleTokenService;
    }

    /**
     * 탈퇴한 회원 행에는 소셜 식별값 해시가 남아 있어 그 값으로 보관한 토큰을 찾는다 (결정 A4-2).
     */
    @TransactionalEventListener
    public void handle(MemberWithdrawn event) {
        try {
            memberRepository.findById(event.memberId())
                    .filter(member -> member.getProvider() == SocialProvider.APPLE)
                    .ifPresent(member -> appleTokenService.revoke(member.getSocialIdHash()));
        } catch (RuntimeException e) {
            log.error("Apple 토큰 취소 처리 실패. memberId={}", event.memberId(), e);
        }
    }
}
