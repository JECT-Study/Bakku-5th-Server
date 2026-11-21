package com.ject.exchange.domain.domain.exchange;

import com.ject.exchange.domain.exception.ExchangeErrorCode;
import com.ject.exchange.domain.exception.ExchangeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
class ExchangeValidateActiveTest {

    private static final Instant NOW = Instant.parse("2026-10-26T05:00:00Z");
    private static final Long EXCHANGE_ID = 1L;
    private static final Long POST_ID = 10L;
    private static final Long REQUESTER_ID = 100L;
    private static final Long OWNER_ID = 200L;

    @Test
    @DisplayName("진행 중인 교환이면 통과한다")
    void validateActive_active_passes() {
        Exchange exchange = exchange(ExchangeStatus.ACTIVE);

        assertDoesNotThrow(exchange::validateActive);
    }

    @ParameterizedTest
    @EnumSource(value = ExchangeStatus.class, names = {"COMPLETED", "CLOSED", "LEFT"})
    @DisplayName("교환이 완료 · 종료 · 나감이면 EXCHANGE_010")
    void validateActive_notActive_throwsExchangeClosed(ExchangeStatus status) {
        Exchange exchange = exchange(status);

        ExchangeException exception = assertThrows(ExchangeException.class, exchange::validateActive);
        assertEquals(ExchangeErrorCode.EXCHANGE_CLOSED, exception.getErrorCode());
    }

    private static Exchange exchange(ExchangeStatus status) {
        Instant completedAt = status == ExchangeStatus.COMPLETED ? NOW : null;
        return Exchange.of(EXCHANGE_ID, POST_ID, REQUESTER_ID, OWNER_ID, OfferType.POST,
                List.of(OfferedItem.of(31L, 501L, null, null, null, 0)),
                status, completedAt, NOW, NOW);
    }
}
