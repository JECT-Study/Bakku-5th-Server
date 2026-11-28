package com.ject.chat.infrastructure.repository;

import com.ject.chat.domain.domain.chatroom.ChatRoom;
import com.ject.chat.domain.domain.message.CardContent;
import com.ject.chat.domain.domain.message.CardType;
import com.ject.chat.domain.domain.message.ChatMessage;
import com.ject.chat.domain.domain.message.ImageContent;
import com.ject.chat.domain.domain.message.SystemContent;
import com.ject.chat.domain.domain.message.SystemLinkType;
import com.ject.chat.domain.domain.message.SystemMessageCode;
import com.ject.chat.domain.domain.message.TextContent;
import com.ject.chat.infrastructure.ChatPostgresTest;
import com.ject.common.jpa.JpaAuditingConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
@Import({JpaChatRoomRepository.class, JpaChatMessageRepository.class, JpaAuditingConfig.class})
class JpaChatMessageRepositoryTest extends ChatPostgresTest {

    @Autowired private JpaChatRoomRepository roomRepository;
    @Autowired private JpaChatMessageRepository messageRepository;
    @Autowired private EntityManager entityManager;
    private Long roomId;

    @BeforeEach
    void setUp() {
        roomId = roomRepository.save(ChatRoom.create(1000L, 2000L, 3000L, 4000L)).getId();
    }

    @Test
    @DisplayName("TEXT IMAGE SYSTEM CARD를 저장하고 타입별 내용을 복원한다")
    void save_roundTripsAllMessageTypes() {
        ChatMessage text = messageRepository.save(ChatMessage.createText(roomId, 3000L, "client-text",
                new TextContent("안녕하세요", false)));
        ChatMessage image = messageRepository.save(ChatMessage.createImage(roomId, 3000L, "client-image",
                new ImageContent(List.of("https://cdn/a", "https://cdn/b"))));
        ChatMessage system = messageRepository.save(ChatMessage.createSystem(roomId,
                new SystemContent(SystemMessageCode.ATTACHED_POST_UPDATED, "수정했어요", SystemLinkType.POST, 20L)));
        ChatMessage card = messageRepository.save(ChatMessage.createCard(roomId,
                new CardContent(CardType.APPOINTMENT_PROPOSAL, 30L)));
        entityManager.flush();
        entityManager.clear();

        List<ChatMessage> restored = messageRepository.findRecent(roomId, null, 10);

        assertEquals(List.of(card.getId(), system.getId(), image.getId(), text.getId()),
                restored.stream().map(ChatMessage::getId).toList());
        ImageContent imageContent = assertInstanceOf(ImageContent.class, restored.get(2).getContent());
        assertEquals(List.of("https://cdn/a", "https://cdn/b"), imageContent.imageUrls());
    }

    @Test
    @DisplayName("recent는 cursor 이전을 내림차순, after는 이후를 오름차순 조회한다")
    void queries_useExclusiveIdBoundariesAndExpectedOrder() {
        ChatMessage first = messageRepository.save(ChatMessage.createText(roomId, 3000L, "first",
                new TextContent("첫째", false)));
        ChatMessage second = messageRepository.save(ChatMessage.createText(roomId, 3000L, "second",
                new TextContent("둘째", false)));
        ChatMessage third = messageRepository.save(ChatMessage.createText(roomId, 3000L, "third",
                new TextContent("셋째", false)));
        entityManager.flush();
        entityManager.clear();

        assertEquals(List.of(second.getId(), first.getId()), messageRepository.findRecent(roomId, third.getId(), 10)
                .stream().map(ChatMessage::getId).toList());
        assertEquals(List.of(second.getId(), third.getId()), messageRepository.findAfter(roomId, first.getId())
                .stream().map(ChatMessage::getId).toList());
    }
}
