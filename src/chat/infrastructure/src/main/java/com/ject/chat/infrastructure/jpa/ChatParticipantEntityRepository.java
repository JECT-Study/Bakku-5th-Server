package com.ject.chat.infrastructure.jpa;

import com.ject.chat.domain.domain.chatroom.ChatParticipantRole;
import com.ject.chat.infrastructure.entity.ChatParticipantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatParticipantEntityRepository extends JpaRepository<ChatParticipantEntity, Long> {

    List<ChatParticipantEntity> findByChatRoomId(Long chatRoomId);

    /**
     * 게시글에서 역할이 role이고 나가지 않은(left_at IS NULL) 참여자 수. countActiveRequestsByPostId(REQUESTER)에 쓴다
     */
    @Query("select count(p) from ChatParticipantEntity p "
            + "where p.chatRoom.postId = :postId and p.role = :role and p.leftAt is null")
    int countByChatRoomPostIdAndRoleAndLeftAtIsNull(@Param("postId") Long postId, @Param("role") ChatParticipantRole role);
}
