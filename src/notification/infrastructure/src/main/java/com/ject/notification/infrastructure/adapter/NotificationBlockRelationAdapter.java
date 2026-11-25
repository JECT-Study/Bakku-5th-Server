package com.ject.notification.infrastructure.adapter;

import com.ject.notification.application.port.BlockRelationPort;
import com.ject.user.application.query.BlockRelationQuery;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 교환글 모듈에도 BlockRelationAdapter가 있어 빈 이름이 겹치지 않게 앞에 Notification을 붙인다.
 */
@Component
public class NotificationBlockRelationAdapter implements BlockRelationPort {

    private final BlockRelationQuery blockRelationQuery;

    public NotificationBlockRelationAdapter(BlockRelationQuery blockRelationQuery) {
        this.blockRelationQuery = blockRelationQuery;
    }

    @Override
    public List<Long> getBlockRelatedMemberIds(Long memberId) {
        return blockRelationQuery.getBlockRelatedMemberIds(memberId);
    }
}
