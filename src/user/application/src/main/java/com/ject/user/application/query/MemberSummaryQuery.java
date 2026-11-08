package com.ject.user.application.query;

import com.ject.user.application.dto.MemberSummary;

import java.util.Collection;
import java.util.List;

/**
 * 다른 BC가 회원 요약을 조회하는 계약. 각 BC는 자기 application에 Port를 두고, infrastructure Adapter에서 이 인터페이스를 호출한다.
 * 구현은 회원 저장소(엔티티 · 마이그레이션)와 함께 추가한다.
 */
public interface MemberSummaryQuery {

    /**
     * 여러 회원의 요약을 한 번에 조회한다 (N+1 방지). 요청한 id마다 하나씩 반환하며 순서는 보장하지 않는다.
     * 탈퇴한 회원도 행이 남아 있어 조회된다.
     *
     * @throws com.ject.user.domain.exception.MemberNotFoundException 없는 회원 id가 있을 때 (MEMBER_201)
     */
    List<MemberSummary> getSummaries(Collection<Long> memberIds);
}
