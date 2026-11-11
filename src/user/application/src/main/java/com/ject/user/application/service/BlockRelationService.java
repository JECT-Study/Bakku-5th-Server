package com.ject.user.application.service;

import com.ject.user.application.query.BlockRelationQuery;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 차단 관계 조회. 차단 API를 구현하기 전까지는 차단 관계가 없는 것으로 응답한다.
 * 다른 BC가 미리 주입해 연동 코드를 작성할 수 있게 계약과 함께 먼저 둔다.
 */
@Service
public class BlockRelationService implements BlockRelationQuery {

    @Override
    public List<Long> getBlockRelatedMemberIds(Long memberId) {
        return List.of();
    }

    @Override
    public boolean isBlocked(Long memberId, Long otherMemberId) {
        return false;
    }
}
