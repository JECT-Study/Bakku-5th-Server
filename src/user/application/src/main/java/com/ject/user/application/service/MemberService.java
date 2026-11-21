package com.ject.user.application.service;

import com.ject.common.core.time.ServiceTime;
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
