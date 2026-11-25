package com.ject.user.application.service;

import com.ject.common.core.time.ServiceTime;
import com.ject.user.application.dto.MarketingConsent;
import com.ject.user.application.dto.MyInfo;
import com.ject.user.application.dto.NicknameChange;
import com.ject.user.application.port.MemberRepository;
import com.ject.user.domain.domain.Member;
import com.ject.user.domain.domain.Nickname;
import com.ject.user.domain.exception.DuplicateNicknameException;
import com.ject.user.domain.exception.MemberNotFoundException;
import com.ject.user.domain.exception.NicknameChangeTooSoonException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

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
     * 닉네임 수정 (MB-3). 30일에 한 번 바꿀 수 있고, 가입 때 정한 닉네임은 횟수에 넣지 않는다 (G-03-2, 결정 D2).
     * 영문 대소문자만 바꾸는 것은 내 닉네임이라 중복으로 보지 않는다.
     *
     * @throws com.ject.user.domain.exception.InvalidNicknameException       형식 오류 (MEMBER_101)
     * @throws DuplicateNicknameException                                    다른 회원이 쓰는 닉네임 (MEMBER_102)
     * @throws com.ject.user.domain.exception.NicknameChangeTooSoonException 30일 안에 다시 변경 (MEMBER_103)
     * @throws com.ject.user.domain.exception.SameNicknameException          지금 닉네임과 같음 (MEMBER_104)
     * @throws MemberNotFoundException                                       없는 회원 · 탈퇴한 회원 (MEMBER_201)
     */
    @Transactional
    public NicknameChange changeNickname(Long memberId, String value) {
        Member member = getActiveMember(memberId);
        Nickname nickname = new Nickname(value);
        memberRepository.findByNickname(nickname)
                .filter(owner -> !owner.getId().equals(memberId))
                .ifPresent(owner -> {
                    throw new DuplicateNicknameException();
                });
        Instant now = serviceTime.now();
        member.changeNickname(nickname, now);
        // 확인과 저장 사이에 다른 요청이 먼저 바꿨으면 조건에 걸려 반영되지 않는다 (결정 C8)
        if (!memberRepository.changeNickname(memberId, nickname, now, now.minus(Member.NICKNAME_CHANGE_INTERVAL))) {
            throw new NicknameChangeTooSoonException(now.plus(Member.NICKNAME_CHANGE_INTERVAL));
        }
        return new NicknameChange(nickname.value(), now.plus(Member.NICKNAME_CHANGE_INTERVAL));
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
