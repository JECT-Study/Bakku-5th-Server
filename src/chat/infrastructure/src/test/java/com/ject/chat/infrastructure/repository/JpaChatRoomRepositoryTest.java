package com.ject.chat.infrastructure.repository;

import com.ject.chat.domain.domain.chatroom.ChatRoom;
import com.ject.chat.infrastructure.ChatPostgresTest;
import com.ject.common.jpa.JpaAuditingConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
@Import({JpaChatRoomRepository.class, JpaAuditingConfig.class})
class JpaChatRoomRepositoryTest extends ChatPostgresTest {

    @Autowired
    private JpaChatRoomRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("채팅방과 요청자·게시자 참여자를 함께 저장하고 복원한다")
    void save_roundTripsRoomAndParticipants() {
        ChatRoom saved = repository.save(ChatRoom.create(1L, 10L, 100L, 200L));
        entityManager.flush();
        entityManager.clear();

        ChatRoom restored = repository.findById(saved.getId()).orElseThrow();

        assertEquals(1L, restored.getExchangeId());
        assertEquals(10L, restored.getPostId());
        assertEquals(100L, restored.getRequesterId());
        assertEquals(2, restored.getParticipants().size());
        assertTrue(restored.isActiveForRequester());
        assertTrue(restored.getParticipants().stream().allMatch(participant -> participant.isNotificationEnabled()
                && participant.getLastReadMessageId() == null));
    }

    @Test
    @DisplayName("같은 게시글과 요청자의 두 번째 채팅방은 DB 유일 제약이 거부한다")
    void save_duplicatePostAndRequester_violatesUniqueConstraint() {
        repository.save(ChatRoom.create(1L, 10L, 100L, 200L));
        entityManager.flush();

        assertThrows(DataIntegrityViolationException.class, () -> {
            repository.save(ChatRoom.create(2L, 10L, 100L, 201L));
            entityManager.flush();
        });
    }
}
