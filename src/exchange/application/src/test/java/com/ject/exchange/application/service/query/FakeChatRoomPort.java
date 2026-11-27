package com.ject.exchange.application.service.query;

import com.ject.exchange.application.dto.dto.ChatRoomInfo;
import com.ject.exchange.application.port.ChatRoomPort;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class FakeChatRoomPort implements ChatRoomPort {

    private final Map<Long, Long> chatRoomIdByExchangeId = new HashMap<>();

    @Override
    public ChatRoomInfo lockAndFind(Long postId, Long requesterId) {
        return new ChatRoomInfo(null, null, ChatRoomInfo.Status.ABSENT);
    }

    @Override
    public Long create(Long exchangeId, Long postId, Long requesterId, Long ownerId) {
        throw new UnsupportedOperationException("조회 테스트에서는 생성하지 않는다.");
    }

    @Override
    public Optional<Long> findChatRoomIdByExchangeId(Long exchangeId) {
        return Optional.ofNullable(chatRoomIdByExchangeId.get(exchangeId));
    }

    void register(Long exchangeId, Long chatRoomId) {
        chatRoomIdByExchangeId.put(exchangeId, chatRoomId);
    }
}
