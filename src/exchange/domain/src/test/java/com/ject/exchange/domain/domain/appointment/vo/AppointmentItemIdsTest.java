package com.ject.exchange.domain.domain.appointment.vo;

import com.ject.exchange.domain.exception.ExchangeErrorCode;
import com.ject.exchange.domain.exception.ExchangeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
class AppointmentItemIdsTest {

    @Test
    @DisplayName("서로 다른 ID 목록이면 그대로 보관한다")
    void create_distinctIds_keepsValues() {
        AppointmentItemIds itemIds = new AppointmentItemIds(List.of(31L, 32L));

        assertEquals(List.of(31L, 32L), itemIds.values());
    }

    @Test
    @DisplayName("원본 목록을 바꿔도 보관한 목록은 바뀌지 않는다")
    void create_copiesValues() {
        List<Long> source = new ArrayList<>(List.of(31L));
        AppointmentItemIds itemIds = new AppointmentItemIds(source);

        source.add(32L);

        assertEquals(List.of(31L), itemIds.values());
        assertThrows(UnsupportedOperationException.class, () -> itemIds.values().add(33L));
    }

    @Test
    @DisplayName("같은 ID가 두 번 있으면 INVALID_OFFERED_ITEMS")
    void create_duplicatedIds_throwsInvalidOfferedItems() {
        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> new AppointmentItemIds(List.of(31L, 31L)));

        assertEquals(ExchangeErrorCode.INVALID_OFFERED_ITEMS, exception.getErrorCode());
    }

    @Test
    @DisplayName("목록이 null이면 INVALID_OFFERED_ITEMS")
    void create_nullList_throwsInvalidOfferedItems() {
        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> new AppointmentItemIds(null));

        assertEquals(ExchangeErrorCode.INVALID_OFFERED_ITEMS, exception.getErrorCode());
    }

    @Test
    @DisplayName("빈 목록이면 INVALID_OFFERED_ITEMS")
    void create_emptyList_throwsInvalidOfferedItems() {
        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> new AppointmentItemIds(List.of()));

        assertEquals(ExchangeErrorCode.INVALID_OFFERED_ITEMS, exception.getErrorCode());
    }

    @Test
    @DisplayName("null 원소가 있으면 INVALID_OFFERED_ITEMS")
    void create_nullElement_throwsInvalidOfferedItems() {
        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> new AppointmentItemIds(Arrays.asList(31L, null)));

        assertEquals(ExchangeErrorCode.INVALID_OFFERED_ITEMS, exception.getErrorCode());
    }
}
