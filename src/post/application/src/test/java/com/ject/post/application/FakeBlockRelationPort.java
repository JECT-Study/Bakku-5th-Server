package com.ject.post.application;

import com.ject.post.application.port.BlockRelationPort;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FakeBlockRelationPort implements BlockRelationPort {

    private final List<Long[]> blocks = new ArrayList<>();

    public FakeBlockRelationPort block(Long blockerId, Long blockedId) {
        blocks.add(new Long[]{blockerId, blockedId});
        return this;
    }

    @Override
    public List<Long> blockRelatedMemberIds(Long memberId) {
        List<Long> related = new ArrayList<>();
        for (Long[] block : blocks) {
            if (Objects.equals(block[0], memberId)) {
                related.add(block[1]);
            }
            if (Objects.equals(block[1], memberId)) {
                related.add(block[0]);
            }
        }
        return related.stream().distinct().toList();
    }

    @Override
    public boolean isBlocked(Long memberId, Long otherMemberId) {
        return blockRelatedMemberIds(memberId).contains(otherMemberId);
    }
}
