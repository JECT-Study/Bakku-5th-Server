package com.ject.user.application.port;

import com.ject.user.domain.domain.Member;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 회원 저장소 출력 포트. 구현은 infrastructure가 담당한다.
 */
public interface MemberRepository {

    Member save(Member member);

    Optional<Member> findById(Long id);

    /**
     * 있는 회원만 반환하며 순서는 보장하지 않는다.
     */
    List<Member> findAllByIds(Collection<Long> ids);

    /**
     * 교환 완료 횟수를 1씩 올린다. 동시에 끝난 교환이 서로 덮어쓰지 않도록 DB에서 직접 더한다 (결정 C8).
     */
    void increaseCompletedCount(Collection<Long> ids);
}
