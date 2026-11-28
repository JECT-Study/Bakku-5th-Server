package com.ject.chat.infrastructure.jpa;

import com.ject.chat.infrastructure.entity.ChatMessageImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageImageEntityRepository extends JpaRepository<ChatMessageImageEntity, Long> {

    /**
     * 메시지별 사진을 순서대로 조회한다. 메시지 여러 건을 한 번에 조회해 N+1을 피한다
     */
    List<ChatMessageImageEntity> findByMessageIdInOrderByMessageIdAscSortOrderAsc(List<Long> messageIds);
}
