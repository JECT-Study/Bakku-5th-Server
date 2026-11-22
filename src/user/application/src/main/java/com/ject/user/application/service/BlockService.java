package com.ject.user.application.service;

import com.ject.common.core.time.ServiceTime;
import com.ject.common.event.member.MemberBlocked;
import com.ject.user.application.dto.BlockResult;
import com.ject.user.application.port.BlockRepository;
import com.ject.user.application.port.MemberRepository;
import com.ject.user.domain.domain.Block;
import com.ject.user.domain.exception.MemberNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;

/**
 * 차단 (BL-1).
 */
@Service
public class BlockService {

    private final BlockRepository blockRepository;
    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ServiceTime serviceTime;

    public BlockService(BlockRepository blockRepository, MemberRepository memberRepository,
                        ApplicationEventPublisher eventPublisher, ServiceTime serviceTime) {
        this.blockRepository = blockRepository;
        this.memberRepository = memberRepository;
        this.eventPublisher = eventPublisher;
        this.serviceTime = serviceTime;
    }

    /**
     * 새로 차단하면 커밋 뒤 MemberBlocked를 받은 교환 · 채팅이 예약중 약속 취소 · 채팅방 숨김을 처리한다 (결정 B4).
     * 이미 차단한 회원이면 이벤트 없이 기존 차단 시각을 돌려준다 (결정 A11).
     *
     * @throws com.ject.user.domain.exception.SelfBlockException 자기 자신 (MEMBER_401)
     * @throws MemberNotFoundException 없는 회원 · 탈퇴한 회원 (MEMBER_201)
     */
    @Transactional
    public BlockResult block(Long blockerId, Long blockedId) {
        // DB(TIMESTAMPTZ)가 마이크로초까지 저장해서, 첫 응답과 다시 차단했을 때의 기존 시각이 같도록 맞춘다
        Block block = Block.create(blockerId, blockedId, serviceTime.now().truncatedTo(ChronoUnit.MICROS));
        memberRepository.findById(blockedId)
                .filter(member -> !member.isWithdrawn())
                .orElseThrow(() -> new MemberNotFoundException(blockedId));
        if (blockRepository.saveIfAbsent(block)) {
            eventPublisher.publishEvent(new MemberBlocked(blockerId, blockedId, block.getBlockedAt()));
            return new BlockResult(blockedId, block.getBlockedAt(), true);
        }
        Block existing = blockRepository.find(blockerId, blockedId).orElseThrow();
        return new BlockResult(blockedId, existing.getBlockedAt(), false);
    }
}
