package com.ject.chat.infrastructure.jpa;

import com.ject.chat.infrastructure.entity.ChatMessageEntity;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageEntityRepository extends JpaRepository<ChatMessageEntity, Long> {

    /**
     * 과거 조회. before보다 작은 메시지를 id 내림차순으로 limit개. cursor가 없으면 before = Long.MAX_VALUE
     */
    @Query("select m from ChatMessageEntity m where m.chatRoomId = :chatRoomId and m.id < :before order by m.id desc")
    List<ChatMessageEntity> findRecent(@Param("chatRoomId") Long chatRoomId, @Param("before") Long before,
                                       Limit limit);

    /**
     * 연결 복구 조회. after보다 큰 메시지를 id 오름차순 전체
     */
    List<ChatMessageEntity> findByChatRoomIdAndIdGreaterThanOrderByIdAsc(Long chatRoomId, Long after);

    boolean existsByIdAndChatRoomId(Long id, Long chatRoomId);
}
