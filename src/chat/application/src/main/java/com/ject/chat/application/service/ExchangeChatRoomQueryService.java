package com.ject.chat.application.service;

import com.ject.chat.application.query.ExchangeChatRoomQuery;
import com.ject.chat.domain.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ExchangeChatRoomQueryService implements ExchangeChatRoomQuery {

    private final ChatRoomRepository chatRoomRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> findChatRoomIdByExchangeId(Long exchangeId) {
        return chatRoomRepository.findChatRoomIdByExchangeId(exchangeId);
    }
}
