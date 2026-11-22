package com.ject.user.application.service;

import com.ject.common.core.time.ServiceTime;
import com.ject.common.event.member.MemberBlocked;
import com.ject.common.event.member.MemberUnblocked;
import com.ject.user.application.dto.BlockResult;
import com.ject.user.application.dto.BlockedMemberPage;
import com.ject.user.application.dto.BlockedMemberPage.BlockedMember;
import com.ject.user.application.port.BlockRepository;
import com.ject.user.application.port.MemberRepository;
import com.ject.user.domain.domain.Block;
import com.ject.user.domain.domain.Member;
import com.ject.user.domain.exception.MemberNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 차단 · 차단 목록 · 차단 해제 (BL-1 · BL-2 · BL-3).
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

    /**
     * 지웠으면 커밋 뒤 MemberUnblocked를 받은 채팅이 숨긴 채팅방을 다시 보이게 한다. 취소된 약속은 되살리지 않는다 (결정 B4).
     * 차단하지 않은 회원이어도 성공으로 처리하고 이벤트는 발행하지 않는다 (결정 A11).
     */
    @Transactional
    public void unblock(Long blockerId, Long blockedId) {
        if (blockRepository.delete(blockerId, blockedId)) {
            eventPublisher.publishEvent(new MemberUnblocked(blockerId, blockedId));
        }
    }

    /**
     * 최근 차단 순. 그사이 탈퇴한 회원은 닉네임이 없어 뺀다 (결정 A10).
     */
    @Transactional(readOnly = true)
    public BlockedMemberPage getBlocks(Long blockerId, Long cursorId, int size) {
        List<Block> rows = blockRepository.findPageOfActiveMembers(blockerId, cursorId, size + 1);
        boolean hasNext = rows.size() > size;
        List<Block> page = hasNext ? rows.subList(0, size) : rows;
        Map<Long, Member> members = memberRepository.findAllByIds(page.stream().map(Block::getBlockedId).toList())
                .stream()
                .collect(Collectors.toMap(Member::getId, Function.identity()));
        List<BlockedMember> content = page.stream()
                .filter(block -> isActive(members.get(block.getBlockedId())))
                .map(block -> new BlockedMember(block.getBlockedId(),
                        members.get(block.getBlockedId()).getNickname().value(), block.getBlockedAt()))
                .toList();
        Long nextCursorId = hasNext ? page.get(page.size() - 1).getId() : null;
        return new BlockedMemberPage(content, nextCursorId, hasNext);
    }

    // 목록 조회와 회원 조회 사이에 탈퇴한 회원
    private static boolean isActive(Member member) {
        return member != null && !member.isWithdrawn();
    }
}
