package com.ject.exchange.infrastructure.repository;

import com.ject.exchange.domain.repository.PostLockRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
@RequiredArgsConstructor
public class PostgresPostLockRepository implements PostLockRepository {

    private final EntityManager entityManager;

    @Override
    public void lockAll(Collection<Long> postIds) {
        // pg_advisory_xact_lock은 void를 돌려주므로 결과 매핑이 되도록 count로 감싼다
        // 여러 글을 잠글 때 순서가 다르면 교착이 생기므로 오름차순으로 잡는다. 트랜잭션이 끝나면 자동으로 풀린다
        postIds.stream()
                .distinct()
                .sorted()
                .forEach(postId -> entityManager
                        .createNativeQuery("select count(*) from (select pg_advisory_xact_lock(:postId)) locked")
                        .setParameter("postId", postId)
                        .getSingleResult());
    }
}
