package com.ject.chat.application.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
class ChatRoomLinkTest {

    @Test
    @DisplayName("방 없음과 현재 방과 나간 방 상태는 계약에 맞는 ID 조합만 허용한다")
    void constructor_acceptsConsistentStates() {
        assertEquals(ChatRoomLinkStatus.ABSENT, new ChatRoomLink(null, null, ChatRoomLinkStatus.ABSENT).status());
        assertEquals(ChatRoomLinkStatus.ACTIVE, new ChatRoomLink(1L, 2L, ChatRoomLinkStatus.ACTIVE).status());
        assertEquals(ChatRoomLinkStatus.LEFT, new ChatRoomLink(1L, null, ChatRoomLinkStatus.LEFT).status());
    }

    @Test
    @DisplayName("상태와 ID 조합이 모순이면 명시적으로 실패한다")
    void constructor_rejectsInconsistentStates() {
        assertThrows(IllegalArgumentException.class,
                () -> new ChatRoomLink(1L, null, ChatRoomLinkStatus.ACTIVE));
        assertThrows(IllegalArgumentException.class,
                () -> new ChatRoomLink(1L, 2L, ChatRoomLinkStatus.LEFT));
        assertThrows(IllegalArgumentException.class,
                () -> new ChatRoomLink(null, 2L, ChatRoomLinkStatus.ABSENT));
        assertThrows(IllegalArgumentException.class,
                () -> new ChatRoomLink(null, null, null));
    }
}
