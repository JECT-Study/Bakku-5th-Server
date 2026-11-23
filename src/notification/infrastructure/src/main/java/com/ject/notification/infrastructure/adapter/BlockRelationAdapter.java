package com.ject.notification.infrastructure.adapter;

import com.ject.notification.application.port.BlockRelationPort;
import com.ject.user.application.query.BlockRelationQuery;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BlockRelationAdapter implements BlockRelationPort {

    private final BlockRelationQuery blockRelationQuery;

    public BlockRelationAdapter(BlockRelationQuery blockRelationQuery) {
        this.blockRelationQuery = blockRelationQuery;
    }

    @Override
    public List<Long> getBlockRelatedMemberIds(Long memberId) {
        return blockRelationQuery.getBlockRelatedMemberIds(memberId);
    }
}
