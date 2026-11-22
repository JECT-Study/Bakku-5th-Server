package com.ject.exchange.domain.domain.exchange;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
class ExchangeCompleteTest {

    private static final Instant CREATED_AT = Instant.parse("2026-11-20T05:00:00Z");
    /** 2026-11-23 15:30 (+09:00) */
    private static final Instant NOW = Instant.parse("2026-11-23T06:30:00Z");
    private static final Long EXCHANGE_ID = 1L;
    private static final Long POST_ID = 10L;
    private static final Long REQUESTER_ID = 100L;
    private static final Long OWNER_ID = 200L;

    @Test
    @DisplayName("진행 중 교환을 완료하면 COMPLETED · 완료 시각 now이고 나머지 값은 그대로다")
    void complete_active_marksCompleted() {
        // given
        Exchange active = Exchange.of(EXCHANGE_ID, POST_ID, REQUESTER_ID, OWNER_ID, OfferType.POST,
                List.of(OfferedItem.of(31L, 501L, null, null, null, 0), OfferedItem.of(32L, 502L, null, null, null, 1)),
                ExchangeStatus.ACTIVE, null, CREATED_AT, CREATED_AT);

        // when
        Exchange completed = active.complete(NOW);

        // then
        assertEquals(ExchangeStatus.COMPLETED, completed.getStatus());
        assertEquals(NOW, completed.getCompletedAt());
        assertEquals(EXCHANGE_ID, completed.getId());
        assertEquals(POST_ID, completed.getPostId());
        assertEquals(REQUESTER_ID, completed.getRequestUserId());
        assertEquals(OWNER_ID, completed.getOwnerId());
        assertEquals(active.getOfferedItems(), completed.getOfferedItems());
    }

    @Test
    @DisplayName("완료는 새 객체를 반환하고 원래 교환은 ACTIVE 그대로다")
    void complete_returnsNewObject_keepsOriginal() {
        Exchange active = Exchange.of(EXCHANGE_ID, POST_ID, REQUESTER_ID, OWNER_ID, OfferType.POST,
                List.of(OfferedItem.of(31L, 501L, null, null, null, 0)),
                ExchangeStatus.ACTIVE, null, CREATED_AT, CREATED_AT);

        active.complete(NOW);

        assertEquals(ExchangeStatus.ACTIVE, active.getStatus());
        assertNull(active.getCompletedAt());
    }
}
