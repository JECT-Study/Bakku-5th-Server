package com.ject.post.infrastructure.adapter;

import com.ject.user.application.query.BlockRelationQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockRelationAdapterTest {

    private final BlockRelationAdapter adapter = new BlockRelationAdapter(new FakeBlockRelationQuery());

    @Test
    @DisplayName("회원과 차단 관계인 회원 id 목록을 회원 도메인에서 그대로 받는다")
    void blockRelatedMemberIds() {
        assertEquals(List.of(7L, 9L), adapter.blockRelatedMemberIds(3L));
    }

    @Test
    @DisplayName("두 회원의 차단 여부를 회원 도메인에서 그대로 받는다")
    void isBlocked() {
        assertTrue(adapter.isBlocked(3L, 7L));
        assertFalse(adapter.isBlocked(3L, 8L));
    }

    private static class FakeBlockRelationQuery implements BlockRelationQuery {

        @Override
        public List<Long> getBlockRelatedMemberIds(Long memberId) {
            return memberId == 3L ? List.of(7L, 9L) : List.of();
        }

        @Override
        public boolean isBlocked(Long memberId, Long otherMemberId) {
            return memberId == 3L && otherMemberId == 7L;
        }
    }
}
