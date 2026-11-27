package com.ject.exchange.api.controller;

import com.ject.exchange.application.dto.dto.ChatRoomInfo;
import com.ject.exchange.application.port.ChatRoomPort;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class FakeChatRoomPort implements ChatRoomPort {

    private final Map<Long, Long> chatRoomIdByExchangeId = new HashMap<>();
    private long nextChatRoomId = 7_000L;

    @Override
    public ChatRoomInfo lockAndFind(Long postId, Long requesterId) {
        return new ChatRoomInfo(null, null, ChatRoomInfo.Status.ABSENT);
    }

    @Override
    public Long create(Long exchangeId, Long postId, Long requesterId, Long ownerId) {
        long chatRoomId = nextChatRoomId++;
        chatRoomIdByExchangeId.put(exchangeId, chatRoomId);
        return chatRoomId;
    }

    @Override
    public Optional<Long> findChatRoomIdByExchangeId(Long exchangeId) {
        return Optional.ofNullable(chatRoomIdByExchangeId.get(exchangeId));
    }
}
