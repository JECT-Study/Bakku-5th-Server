package com.ject.chat.application.port;

import com.ject.chat.application.dto.ChatExchangeState;

/**
 * exchange BC의 교환 · 약속 상태를 조회하는 Port
 */
public interface ChatExchangePort {

    /**
     * 채팅방 상세 판정에 필요한 교환 상태를 조회한다
     *
     * @param exchangeId 이 방에 연결된 교환 ID
     * @param viewerId   조회 회원 ID
     */
    ChatExchangeState getExchangeState(Long exchangeId, Long viewerId);
}
