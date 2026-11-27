package com.ject.chat.application;

import com.ject.chat.application.dto.PostChatRoomInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostChatRoomInfoTest {

    @Test
    @DisplayName("현재 채팅방이 있으면 방 ID를 주고 나간 이력은 없다")
    void activeChatRoom() {
        // when
        PostChatRoomInfo info = new PostChatRoomInfo(31L, false, 2);

        // then
        assertEquals(31L, info.chatRoomId());
        assertFalse(info.hasLeftChatRoom());
        assertEquals(2, info.requestCount());
    }

    @Test
    @DisplayName("요청한 적이 없으면 방 ID와 나간 이력이 모두 없다")
    void neverRequested() {
        // when
        PostChatRoomInfo info = new PostChatRoomInfo(null, false, 1);

        // then
        assertNull(info.chatRoomId());
        assertFalse(info.hasLeftChatRoom());
    }

    @Test
    @DisplayName("나간 방은 다시 들어갈 방 ID 없이 나간 이력만 남는다")
    void leftChatRoom() {
        // when
        PostChatRoomInfo info = new PostChatRoomInfo(null, true, 1);

        // then
        assertNull(info.chatRoomId());
        assertTrue(info.hasLeftChatRoom());
    }

    @Test
    @DisplayName("유효한 요청이 없으면 요청 수 0을 허용한다")
    void noActiveRequest() {
        // when
        PostChatRoomInfo info = new PostChatRoomInfo(null, false, 0);

        // then
        assertEquals(0, info.requestCount());
    }

    @Test
    @DisplayName("나간 방에 현재 방 ID가 함께 있으면 거부한다")
    void leftChatRoomWithActiveRoomId() {
        assertThrows(IllegalArgumentException.class, () -> new PostChatRoomInfo(31L, true, 1));
    }

    @Test
    @DisplayName("요청 수가 음수면 거부한다")
    void negativeRequestCount() {
        assertThrows(IllegalArgumentException.class, () -> new PostChatRoomInfo(null, false, -1));
    }
}
