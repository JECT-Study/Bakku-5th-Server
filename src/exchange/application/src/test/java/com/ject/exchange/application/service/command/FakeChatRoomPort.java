package com.ject.exchange.application.service.command;

import com.ject.exchange.application.dto.dto.ChatRoomInfo;
import com.ject.exchange.application.port.ChatRoomPort;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class FakeChatRoomPort implements ChatRoomPort {

    private static final long FIRST_CHAT_ROOM_ID = 7_000L;

    private final Map<Long, Long> chatRoomIdByExchangeId = new HashMap<>();
    private ChatRoomInfo link = new ChatRoomInfo(null, null, ChatRoomInfo.Status.ABSENT);
    private int createCount;
    private long nextChatRoomId = FIRST_CHAT_ROOM_ID;
    private RuntimeException createFailure;

    @Override
    public ChatRoomInfo lockAndFind(Long postId, Long requesterId) {
        return link;
    }

    @Override
    public Long create(Long exchangeId, Long postId, Long requesterId, Long ownerId) {
        if (createFailure != null) {
            throw createFailure;
        }
        createCount++;
        long chatRoomId = nextChatRoomId++;
        chatRoomIdByExchangeId.put(exchangeId, chatRoomId);
        return chatRoomId;
    }

    @Override
    public Optional<Long> findChatRoomIdByExchangeId(Long exchangeId) {
        return Optional.ofNullable(chatRoomIdByExchangeId.get(exchangeId));
    }

    void active(Long exchangeId, Long chatRoomId) {
        link = new ChatRoomInfo(exchangeId, chatRoomId, ChatRoomInfo.Status.ACTIVE);
        chatRoomIdByExchangeId.put(exchangeId, chatRoomId);
    }

    void left(Long exchangeId) {
        link = new ChatRoomInfo(exchangeId, null, ChatRoomInfo.Status.LEFT);
    }

    void failCreate(RuntimeException failure) {
        createFailure = failure;
    }

    int createCount() {
        return createCount;
    }
}
