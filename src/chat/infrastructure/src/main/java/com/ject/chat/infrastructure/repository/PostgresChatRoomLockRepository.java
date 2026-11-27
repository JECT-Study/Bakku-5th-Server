package com.ject.chat.infrastructure.repository;

import com.ject.chat.domain.repository.ChatRoomLockRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PostgresChatRoomLockRepository implements ChatRoomLockRepository {

    private final EntityManager entityManager;

    @Override
    public void lock(Long postId, Long requesterId) {
        // pg_advisory_xact_lock은 void를 돌려주므로 결과 매핑이 되도록 count로 감싼다
        // (postId, requesterId) 조합을 hashtextextended로 안정적인 단일 bigint 키로 만든다. 해시 충돌은 과도한 직렬화만 일으키고
        // 중복 생성 안전성은 DB unique 제약이 최종적으로 지킨다. 트랜잭션이 끝나면 자동으로 풀린다
        entityManager
                .createNativeQuery("select count(*) from (select pg_advisory_xact_lock("
                        + "hashtextextended(CAST(:postId AS text) || ':' || CAST(:requesterId AS text), 0))) locked")
                .setParameter("postId", postId)
                .setParameter("requesterId", requesterId)
                .getSingleResult();
    }
}
