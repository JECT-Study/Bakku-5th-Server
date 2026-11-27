package com.ject.chat.application.query;

import java.util.Optional;

/**
 * 다른 BC가 교환 ID로 연결된 채팅방 ID를 조회하는 application 공개 계약. 각 BC는 자기 application에 Port를 두고,
 * infrastructure Adapter에서 이 인터페이스를 호출한다.
 */
public interface ExchangeChatRoomQuery {

    /**
     * 교환에 연결된 채팅방 ID를 조회한다. 아직 없으면 empty
     *
     * @param exchangeId 교환 ID. null이 아님을 전제한다
     * @return 채팅방 ID. 없으면 empty
     */
    Optional<Long> findChatRoomIdByExchangeId(Long exchangeId);
}
