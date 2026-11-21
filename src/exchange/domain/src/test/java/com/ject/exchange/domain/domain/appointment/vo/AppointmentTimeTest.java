package com.ject.exchange.domain.domain.appointment.vo;

import com.ject.exchange.domain.domain.appointment.AppointmentTimeType;
import com.ject.exchange.domain.exception.ExchangeErrorCode;
import com.ject.exchange.domain.exception.ExchangeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
class AppointmentTimeTest {

    /** 공통 Given의 지금 시각 (서비스 시간대 +09:00) */
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-10-26T14:00:00+09:00");

    @Test
    @DisplayName("SCHEDULED이고 미래 · 10분 단위 · 7일 이내면 입력 시각 그대로 만들어진다")
    void of_validScheduled_keepsScheduledAt() {
        AppointmentTime time = AppointmentTime.of(AppointmentTimeType.SCHEDULED,
                OffsetDateTime.parse("2026-10-27T15:00:00+09:00"), NOW);

        assertEquals(AppointmentTimeType.SCHEDULED, time.type());
        assertEquals(Instant.parse("2026-10-27T06:00:00Z"), time.scheduledAt());
    }

    @Test
    @DisplayName("지금 바로 다음 10분 칸(14:10)은 허용된다")
    void of_nextTenMinuteSlot_isAllowed() {
        AppointmentTime time = AppointmentTime.of(AppointmentTimeType.SCHEDULED,
                OffsetDateTime.parse("2026-10-26T14:10:00+09:00"), NOW);

        assertEquals(Instant.parse("2026-10-26T05:10:00Z"), time.scheduledAt());
    }

    @Test
    @DisplayName("NOW면 약속 시각이 null이다")
    void of_now_hasNullScheduledAt() {
        AppointmentTime time = AppointmentTime.of(AppointmentTimeType.NOW, null, NOW);

        assertEquals(AppointmentTimeType.NOW, time.type());
        assertNull(time.scheduledAt());
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-10-27T15:00:00+09:00", "2026-10-26T13:00:00+09:00", "2026-10-27T15:05:30+09:00"})
    @DisplayName("NOW에 시각을 같이 보내면 오류 없이 시각을 버린다")
    void of_nowWithScheduledAt_ignoresScheduledAt(String scheduledAt) {
        AppointmentTime time = AppointmentTime.of(AppointmentTimeType.NOW, OffsetDateTime.parse(scheduledAt), NOW);

        assertEquals(AppointmentTimeType.NOW, time.type());
        assertNull(time.scheduledAt());
    }

    @Test
    @DisplayName("SCHEDULED인데 시각이 없으면 INVALID_SCHEDULED_AT")
    void of_scheduledWithoutTime_throwsInvalidScheduledAt() {
        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> AppointmentTime.of(AppointmentTimeType.SCHEDULED, null, NOW));

        assertEquals(ExchangeErrorCode.INVALID_SCHEDULED_AT, exception.getErrorCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-10-26T14:00:00+09:00", "2026-10-26T13:50:00+09:00", "2026-10-25T15:00:00+09:00"})
    @DisplayName("SCHEDULED 시각이 지금이거나 그 이전이면 INVALID_SCHEDULED_AT")
    void of_notFuture_throwsInvalidScheduledAt(String scheduledAt) {
        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> AppointmentTime.of(AppointmentTimeType.SCHEDULED, OffsetDateTime.parse(scheduledAt), NOW));

        assertEquals(ExchangeErrorCode.INVALID_SCHEDULED_AT, exception.getErrorCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-10-27T15:05:00+09:00", "2026-10-27T15:01:00+09:00", "2026-10-27T15:00:30+09:00",
            "2026-10-27T15:00:00.000000001+09:00", "2026-10-27T15:00:00.5+09:00"})
    @DisplayName("분이 10의 배수가 아니거나 초 · 나노초가 0이 아니면 INVALID_SCHEDULED_AT")
    void of_notTenMinuteStep_throwsInvalidScheduledAt(String scheduledAt) {
        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> AppointmentTime.of(AppointmentTimeType.SCHEDULED, OffsetDateTime.parse(scheduledAt), NOW));

        assertEquals(ExchangeErrorCode.INVALID_SCHEDULED_AT, exception.getErrorCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-11-02T23:50:00+09:00", "2026-11-02T00:00:00+09:00", "2026-10-26T23:50:00+09:00"})
    @DisplayName("오늘부터 7일 후 23:50까지는 허용된다")
    void of_withinSevenDays_isAllowed(String scheduledAt) {
        OffsetDateTime input = OffsetDateTime.parse(scheduledAt);

        AppointmentTime time = AppointmentTime.of(AppointmentTimeType.SCHEDULED, input, NOW);

        assertEquals(input.toInstant(), time.scheduledAt());
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-11-03T00:00:00+09:00", "2026-11-03T10:00:00+09:00", "2026-11-10T15:00:00+09:00"})
    @DisplayName("8일 후 날짜부터는 INVALID_SCHEDULED_AT")
    void of_afterSevenDays_throwsInvalidScheduledAt(String scheduledAt) {
        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> AppointmentTime.of(AppointmentTimeType.SCHEDULED, OffsetDateTime.parse(scheduledAt), NOW));

        assertEquals(ExchangeErrorCode.INVALID_SCHEDULED_AT, exception.getErrorCode());
    }
}
