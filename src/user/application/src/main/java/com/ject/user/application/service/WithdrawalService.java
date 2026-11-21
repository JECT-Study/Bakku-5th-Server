package com.ject.user.application.service;

import com.ject.common.core.time.ServiceTime;
import com.ject.common.event.member.MemberWithdrawn;
import com.ject.user.application.port.MemberRepository;
import com.ject.user.application.port.PushDeviceRepository;
import com.ject.user.application.port.RefreshTokenRepository;
import com.ject.user.application.port.WithdrawalReasonRepository;
import com.ject.user.domain.domain.Member;
import com.ject.user.domain.domain.WithdrawalReason;
import com.ject.user.domain.exception.MemberNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 회원 탈퇴 (MB-6). 진행 중인 교환이 있어도 탈퇴할 수 있다 (결정 B5).
 * 카카오 · 네이버 연결 끊기는 FE가 SDK로 먼저 하고 서버는 하지 않는다 (결정 C17).
 */
@Service
public class WithdrawalService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PushDeviceRepository pushDeviceRepository;
    private final WithdrawalReasonRepository withdrawalReasonRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ServiceTime serviceTime;

    public WithdrawalService(MemberRepository memberRepository, RefreshTokenRepository refreshTokenRepository,
                             PushDeviceRepository pushDeviceRepository,
                             WithdrawalReasonRepository withdrawalReasonRepository,
                             ApplicationEventPublisher eventPublisher, ServiceTime serviceTime) {
        this.memberRepository = memberRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.pushDeviceRepository = pushDeviceRepository;
        this.withdrawalReasonRepository = withdrawalReasonRepository;
        this.eventPublisher = eventPublisher;
        this.serviceTime = serviceTime;
    }

    /**
     * 닉네임 · 생년월일 · 성별을 비우고 WITHDRAWN으로 바꾼 뒤(결정 A4-2), 모든 기기의 리프레시 · 푸시 토큰을 지운다.
     * 약속 취소 · 글 삭제 · 채팅방 나가기는 커밋 뒤 MemberWithdrawn을 받은 각 파트가 처리한다 (결정 B6).
     *
     * @param reason 선택, null 가능
     * @throws MemberNotFoundException 없는 회원 · 이미 탈퇴한 회원 (MEMBER_201, 인증 필터와 같음)
     */
    @Transactional
    public void withdraw(Long memberId, String reason) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));
        if (member.isWithdrawn()) {
            throw new MemberNotFoundException(memberId);
        }
        Instant now = serviceTime.now();
        member.withdraw(now);
        memberRepository.save(member);
        refreshTokenRepository.deleteAllByMemberId(memberId);
        pushDeviceRepository.deleteAllByMemberId(memberId);
        if (reason != null) {
            withdrawalReasonRepository.save(memberId, WithdrawalReason.valueOf(reason), now);
        }
        eventPublisher.publishEvent(new MemberWithdrawn(memberId, now));
    }
}
