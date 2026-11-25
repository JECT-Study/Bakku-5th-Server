package com.ject.user.application.query;

import com.ject.user.application.dto.MemberRestriction;

/**
 * 다른 BC가 회원의 이용 제한 여부를 조회하는 계약. 게시글 작성 · 수정처럼 쓰기 동작을 막을 때 쓴다 (P-05-01).
 */
public interface MemberRestrictionQuery {

    /**
     * 해제 일시가 지난 제한은 제한이 아닌 것으로 본다.
     *
     * @throws com.ject.user.domain.exception.MemberNotFoundException 없는 회원 id일 때 (MEMBER_201)
     */
    MemberRestriction getRestriction(Long memberId);
}
