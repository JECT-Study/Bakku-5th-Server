package com.ject.user.application;

import com.ject.user.application.service.WithdrawalNoticeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WithdrawalNoticeServiceTest {

    private static final Map<Long, Integer> CONFIRMED_APPOINTMENTS = Map.of(101L, 2);

    private final WithdrawalNoticeService withdrawalNoticeService = new WithdrawalNoticeService(
            memberId -> CONFIRMED_APPOINTMENTS.getOrDefault(memberId, 0));

    @Test
    @DisplayName("MB-5 진행 중인 교환(확정 약속) 수를 준다")
    void countInProgressExchanges() {
        assertEquals(2, withdrawalNoticeService.countInProgressExchanges(101L));
    }

    @Test
    @DisplayName("MB-5 진행 중인 교환이 없으면 0이다")
    void noInProgressExchanges() {
        assertEquals(0, withdrawalNoticeService.countInProgressExchanges(202L));
    }
}
