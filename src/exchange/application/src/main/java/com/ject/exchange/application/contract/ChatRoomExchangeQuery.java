package com.ject.exchange.application.contract;

/**
 * chat BC가 채팅방 상세에서 교환 · 약속 상태를 조회하는 계약. 각 BC는 자기 application에 Port를 두고,
 * infrastructure Adapter에서 이 인터페이스를 호출한다. 구현은 기존 교환 · 약속 참여 권한을 다시 검증한다.
 */
public interface ChatRoomExchangeQuery {

    /**
     * 채팅방이 연결된 교환의 현재 상태를 조회한다
     *
     * @param exchangeId 교환 ID. null이 아님을 전제한다
     * @param viewerId   조회 회원 ID. 교환 참여자가 아니면 {@link com.ject.exchange.domain.exception.ExchangeException}
     *                    ({@code ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT})
     */
    ChatRoomExchangeState getChatRoomState(Long exchangeId, Long viewerId);
}
