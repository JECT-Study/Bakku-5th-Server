package com.ject.exchange.domain.domain.exchange;

import com.ject.exchange.domain.exception.ExchangeErrorCode;
import com.ject.exchange.domain.exception.ExchangeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
class ExchangeCounterpartTest {

    private static final Instant NOW = Instant.parse("2026-10-26T05:00:00Z");
    private static final Long EXCHANGE_ID = 1L;
    private static final Long POST_ID = 10L;
    private static final Long REQUESTER_ID = 100L;
    private static final Long OWNER_ID = 200L;
    private static final Long OTHER_MEMBER_ID = 300L;

    @Test
    @DisplayName("요청자의 상대는 게시자다")
    void counterpartOf_requester_returnsOwner() {
        assertEquals(OWNER_ID, exchange().counterpartOf(REQUESTER_ID));
    }

    @Test
    @DisplayName("게시자의 상대는 요청자다")
    void counterpartOf_owner_returnsRequester() {
        assertEquals(REQUESTER_ID, exchange().counterpartOf(OWNER_ID));
    }

    @Test
    @DisplayName("참여자가 아니면 NOT_EXCHANGE_PARTICIPANT")
    void counterpartOf_thirdParty_throwsNotExchangeParticipant() {
        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> exchange().counterpartOf(OTHER_MEMBER_ID));

        assertEquals(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT, exception.getErrorCode());
    }

    private static Exchange exchange() {
        return Exchange.of(EXCHANGE_ID, POST_ID, REQUESTER_ID, OWNER_ID, OfferType.POST,
                List.of(OfferedItem.of(31L, 501L, null, null, null, 0)),
                ExchangeStatus.ACTIVE, null, NOW, NOW);
    }
}
