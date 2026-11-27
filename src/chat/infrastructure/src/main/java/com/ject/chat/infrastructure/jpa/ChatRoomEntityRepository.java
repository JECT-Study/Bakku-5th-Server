package com.ject.chat.infrastructure.jpa;

import com.ject.chat.infrastructure.entity.ChatRoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChatRoomEntityRepository extends JpaRepository<ChatRoomEntity, Long> {

    /**
     * 동일 게시글·동일 요청자가 만든 방을 찾는다. 나간 방도 포함한다
     */
    Optional<ChatRoomEntity> findByPostIdAndRequesterId(Long postId, Long requesterId);

    // 엔티티를 영속성 컨텍스트에 올리지 않도록 id만 읽는다
    @Query("select r.id from ChatRoomEntity r where r.exchangeId = :exchangeId")
    Optional<Long> findIdByExchangeId(@Param("exchangeId") Long exchangeId);
}
