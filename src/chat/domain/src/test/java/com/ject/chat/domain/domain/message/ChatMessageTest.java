package com.ject.chat.domain.domain.message;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
class ChatMessageTest {

    @Test
    @DisplayName("TEXT 메시지는 공백이 아닌 1~1,000자만 허용한다")
    void textContent_validatesLengthAndBlank() {
        assertEquals(1, new TextContent("가", false).text().length());
        assertEquals(1000, new TextContent("가".repeat(1000), false).text().length());
        assertThrows(IllegalArgumentException.class, () -> new TextContent(" ", false));
        assertThrows(IllegalArgumentException.class, () -> new TextContent("가".repeat(1001), false));
    }

    @Test
    @DisplayName("IMAGE 메시지는 URL 1~5개의 순서와 방어 복사를 보존한다")
    void imageContent_validatesCountAndDefensivelyCopies() {
        List<String> source = new ArrayList<>(List.of("a", "b"));
        ImageContent content = new ImageContent(source);
        source.add("c");

        assertEquals(List.of("a", "b"), content.imageUrls());
        assertThrows(UnsupportedOperationException.class, () -> content.imageUrls().add("d"));
        assertThrows(IllegalArgumentException.class, () -> new ImageContent(List.of()));
        assertThrows(IllegalArgumentException.class, () -> new ImageContent(List.of("1", "2", "3", "4", "5", "6")));
        assertThrows(IllegalArgumentException.class, () -> new ImageContent(List.of(" ")));
    }

    @Test
    @DisplayName("SYSTEM link는 종류와 ID가 함께 있거나 함께 없어야 한다")
    void systemContent_validatesLinkPair() {
        SystemContent withoutLink = new SystemContent(SystemMessageCode.COUNTERPART_LEFT, "상대가 나갔어요", null, null);

        assertNull(withoutLink.linkType());
        assertThrows(IllegalArgumentException.class, () -> new SystemContent(
                SystemMessageCode.ATTACHED_POST_UPDATED, "수정됐어요", SystemLinkType.POST, null));
    }

    @Test
    @DisplayName("SYSTEM과 CARD 메시지는 sender와 clientMessageId를 가지지 않는다")
    void createServerMessages_haveNoSenderOrClientMessageId() {
        ChatMessage system = ChatMessage.createSystem(5L,
                new SystemContent(SystemMessageCode.COUNTERPART_LEFT, "상대가 나갔어요", null, null));
        ChatMessage card = ChatMessage.createCard(5L, new CardContent(CardType.APPOINTMENT_PROPOSAL, 3L));

        assertNull(system.getSenderUserId());
        assertNull(system.getClientMessageId());
        assertNull(card.getSenderUserId());
        assertNull(card.getClientMessageId());
    }

    @Test
    @DisplayName("영속 복원 시 타입과 내용이 다르면 손상 데이터로 거부한다")
    void restore_mismatchedTypeAndContent_throws() {
        assertThrows(IllegalArgumentException.class, () -> ChatMessage.of(
                1L, 5L, 101L, ChatMessageType.TEXT, new ImageContent(List.of("a")), "client", Instant.EPOCH,
                Instant.EPOCH));
    }

    @Test
    @DisplayName("영속 복원 시 SYSTEM에 sender나 clientMessageId가 있으면 손상 데이터로 거부한다")
    void restore_serverMessageWithUserIdentity_throws() {
        SystemContent content = new SystemContent(SystemMessageCode.COUNTERPART_LEFT, "상대가 나갔어요", null, null);

        assertThrows(IllegalArgumentException.class, () -> ChatMessage.of(
                1L, 5L, 101L, ChatMessageType.SYSTEM, content, null, Instant.EPOCH, Instant.EPOCH));
        assertThrows(IllegalArgumentException.class, () -> ChatMessage.of(
                1L, 5L, null, ChatMessageType.SYSTEM, content, "client", Instant.EPOCH, Instant.EPOCH));
    }
}
