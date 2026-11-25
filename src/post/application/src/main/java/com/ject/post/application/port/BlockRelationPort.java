package com.ject.post.application.port;

import java.util.List;

public interface BlockRelationPort {

    // 내가 차단했거나 나를 차단한 회원 (양방향)
    List<Long> blockRelatedMemberIds(Long memberId);

    boolean isBlocked(Long memberId, Long otherMemberId);
}
