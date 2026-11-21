package com.ject.user.application.service;

import com.ject.common.core.time.ServiceTime;
import com.ject.user.application.dto.MarketingConsent;
import com.ject.user.application.dto.MyInfo;
import com.ject.user.application.port.MemberRepository;
import com.ject.user.domain.domain.Member;
import com.ject.user.domain.exception.MemberNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인한 회원 본인의 정보 조회 · 변경.
 */
@Service
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final ServiceTime serviceTime;

    public MemberService(MemberRepository memberRepository, ServiceTime serviceTime) {
        this.memberRepository = memberRepository;
        this.serviceTime = serviceTime;
    }

    /**
     * 내 정보 (MB-2).
     *
     * @throws MemberNotFoundException 없는 회원 · 탈퇴한 회원 (MEMBER_201)
     */
    public MyInfo getMyInfo(Long memberId) {
        return MyInfo.from(getActiveMember(memberId), serviceTime.now());
    }

    /**
     * 마케팅 정보 수신 동의 변경 (MB-7). 같은 값이면 변경 일시를 바꾸지 않는다.
     *
     * @throws MemberNotFoundException 없는 회원 · 탈퇴한 회원 (MEMBER_201)
     */
    @Transactional
    public MarketingConsent updateMarketingConsent(Long memberId, boolean agreed) {
        Member member = getActiveMember(memberId);
        member.changeMarketingConsent(agreed, serviceTime.now());
        return MarketingConsent.from(memberRepository.save(member));
    }

    /**
     * 인증 필터에서 이미 막지만, 필터 확인 뒤 탈퇴가 끝난 요청도 같은 오류로 처리한다 (AuthenticationService와 같음).
     */
    private Member getActiveMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));
        if (member.isWithdrawn()) {
            throw new MemberNotFoundException(memberId);
        }
        return member;
    }
}
