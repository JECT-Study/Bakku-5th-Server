package com.ject.user.application.query;

import java.util.List;

/**
 * 다른 BC가 차단 관계를 조회하는 계약. 교환글은 목록 · 상세에서, 교환 · 채팅은 요청 · 채팅방에서 차단한 회원을 거른다.
 * 차단은 양방향으로 본다: 내가 차단했거나 나를 차단한 회원 모두 해당한다 (C-09-2).
 */
public interface BlockRelationQuery {

    /**
     * 이 회원과 차단 관계인 회원 id 목록 (양방향).
     */
    List<Long> getBlockRelatedMemberIds(Long memberId);

    /**
     * 두 회원 중 한쪽이라도 상대를 차단했는지.
     */
    boolean isBlocked(Long memberId, Long otherMemberId);
}
