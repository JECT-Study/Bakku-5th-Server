package com.ject.exchange.infrastructure.adapter;

import com.ject.chat.application.command.ChatRoomCommand;
import com.ject.chat.application.dto.ChatRoomLink;
import com.ject.chat.application.dto.ChatRoomLinkStatus;
import com.ject.chat.application.query.ExchangeChatRoomQuery;
import com.ject.exchange.application.dto.dto.ChatRoomInfo;
import com.ject.exchange.application.port.ChatRoomPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * chat BC의 application 공개 계약(ChatRoomCommand · ExchangeChatRoomQuery)을 호출해 exchange 관점의
 * {@link ChatRoomInfo}로 매핑한다. 잠금·조회·생성은 호출한 exchange transaction 안에서 그대로 실행된다
 */
@Component
@RequiredArgsConstructor
public class ChatRoomAdapter implements ChatRoomPort {

    private final ChatRoomCommand chatRoomCommand;
    private final ExchangeChatRoomQuery exchangeChatRoomQuery;

    @Override
    public ChatRoomInfo lockAndFind(Long postId, Long requesterId) {
        return toChatRoomInfo(chatRoomCommand.lockAndFind(postId, requesterId));
    }

    @Override
    public Long create(Long exchangeId, Long postId, Long requesterId, Long ownerId) {
        return chatRoomCommand.create(exchangeId, postId, requesterId, ownerId);
    }

    @Override
    public Optional<Long> findChatRoomIdByExchangeId(Long exchangeId) {
        return exchangeChatRoomQuery.findChatRoomIdByExchangeId(exchangeId);
    }

    private static ChatRoomInfo toChatRoomInfo(ChatRoomLink link) {
        return new ChatRoomInfo(link.exchangeId(), link.chatRoomId(), toStatus(link.status()));
    }

    private static ChatRoomInfo.Status toStatus(ChatRoomLinkStatus status) {
        return switch (status) {
            case ABSENT -> ChatRoomInfo.Status.ABSENT;
            case ACTIVE -> ChatRoomInfo.Status.ACTIVE;
            case LEFT -> ChatRoomInfo.Status.LEFT;
        };
    }
}
