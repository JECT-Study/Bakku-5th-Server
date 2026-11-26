package com.ject.post.infrastructure.adapter;

import com.ject.user.application.dto.MemberRestriction;
import com.ject.user.application.query.MemberRestrictionQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemberRestrictionAdapterTest {

    private static final Long RESTRICTED_ID = 3L;
    private static final Long NORMAL_ID = 4L;

    private final MemberRestrictionAdapter adapter = new MemberRestrictionAdapter(new FakeMemberRestrictionQuery());

    @Test
    @DisplayName("회원 도메인의 이용 제한 여부를 그대로 받는다")
    void isRestricted() {
        assertTrue(adapter.isRestricted(RESTRICTED_ID));
        assertFalse(adapter.isRestricted(NORMAL_ID));
    }

    private static class FakeMemberRestrictionQuery implements MemberRestrictionQuery {

        @Override
        public MemberRestriction getRestriction(Long memberId) {
            if (RESTRICTED_ID.equals(memberId)) {
                return new MemberRestriction(true, MemberRestriction.RestrictionReason.NO_SHOW,
                        Instant.parse("2026-12-01T00:00:00Z"));
            }
            return MemberRestriction.none();
        }
    }
}
