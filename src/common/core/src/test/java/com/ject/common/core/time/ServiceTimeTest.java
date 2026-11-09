package com.ject.common.core.time;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ServiceTimeTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    // UTC로는 11/5 15:00이지만 한국 시간으로는 11/6 00:00
    private static final Instant KOREAN_MIDNIGHT = Instant.parse("2026-11-05T15:00:00Z");

    private final ServiceTime serviceTime = new ServiceTime(Clock.fixed(KOREAN_MIDNIGHT, SEOUL));

    @Test
    @DisplayName("현재 시각은 시간대와 상관없이 같은 Instant다")
    void now() {
        // when
        Instant now = serviceTime.now();

        // then
        assertEquals(KOREAN_MIDNIGHT, now);
    }

    @Test
    @DisplayName("오늘 날짜는 UTC가 아니라 서비스 시간대 기준이다")
    void today() {
        // when
        LocalDate today = serviceTime.today();

        // then
        assertEquals(LocalDate.of(2026, 11, 6), today);
    }

    @Test
    @DisplayName("응답용 시각은 서비스 시간대의 오프셋으로 변환한다")
    void toOffset() {
        // when
        OffsetDateTime result = serviceTime.toOffset(KOREAN_MIDNIGHT);

        // then
        assertEquals(OffsetDateTime.parse("2026-11-06T00:00:00+09:00"), result);
    }

    @Test
    @DisplayName("비어 있는 시각은 null로 변환한다")
    void toOffsetNull() {
        assertNull(serviceTime.toOffset(null));
    }
}
