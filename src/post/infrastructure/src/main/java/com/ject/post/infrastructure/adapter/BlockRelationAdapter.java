package com.ject.post.infrastructure.adapter;

import com.ject.post.application.port.BlockRelationPort;
import com.ject.user.application.query.BlockRelationQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BlockRelationAdapter implements BlockRelationPort {

    private final BlockRelationQuery blockRelationQuery;

    @Override
    public List<Long> blockRelatedMemberIds(Long memberId) {
        return blockRelationQuery.getBlockRelatedMemberIds(memberId);
    }

    @Override
    public boolean isBlocked(Long memberId, Long otherMemberId) {
        return blockRelationQuery.isBlocked(memberId, otherMemberId);
    }
}
