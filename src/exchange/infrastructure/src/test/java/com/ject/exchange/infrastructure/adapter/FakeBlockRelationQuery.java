package com.ject.exchange.infrastructure.adapter;

import com.ject.user.application.query.BlockRelationQuery;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 테스트용 가짜 차단 관계 조회. user BC 계약대로 한쪽이라도 차단했으면 양방향으로 차단 관계다.
 */
class FakeBlockRelationQuery implements BlockRelationQuery {

    private final Set<List<Long>> blocks = new HashSet<>();
    private final List<List<Long>> isBlockedCalls = new ArrayList<>();

    @Override
    public List<Long> getBlockRelatedMemberIds(Long memberId) {
        Set<Long> related = new HashSet<>();
        for (List<Long> block : blocks) {
            if (block.get(0).equals(memberId)) {
                related.add(block.get(1));
            }
            if (block.get(1).equals(memberId)) {
                related.add(block.get(0));
            }
        }
        return List.copyOf(related);
    }

    @Override
    public boolean isBlocked(Long memberId, Long otherMemberId) {
        isBlockedCalls.add(List.of(memberId, otherMemberId));
        return blocks.contains(List.of(memberId, otherMemberId))
                || blocks.contains(List.of(otherMemberId, memberId));
    }

    void block(Long blockerId, Long blockedId) {
        blocks.add(List.of(blockerId, blockedId));
    }

    List<List<Long>> isBlockedCalls() {
        return isBlockedCalls;
    }
}
