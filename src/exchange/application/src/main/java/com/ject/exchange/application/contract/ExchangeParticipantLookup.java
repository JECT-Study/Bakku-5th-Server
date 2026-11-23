package com.ject.exchange.application.contract;

/**
 * 다른 BC(image 등)가 회원이 교환 참여자인지 확인하는 계약
 */
public interface ExchangeParticipantLookup {

    /**
     * 요청자 · 게시자면 true. 아니거나 교환이 없으면 false
     */
    boolean isParticipant(Long exchangeId, Long memberId);
}
