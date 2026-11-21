package com.ject.exchange.domain.domain.appointment.vo;

import com.ject.exchange.domain.domain.appointment.AppointmentTimeType;
import com.ject.exchange.domain.exception.ExchangeErrorCode;
import com.ject.exchange.domain.exception.ExchangeException;

import java.time.Instant;
import java.time.OffsetDateTime;

/**
 * 약속 시간. 지금 바로면 시각이 없고(null), 날짜 · 시간 지정이면 미래 · 10분 단위 · 오늘~7일 후 시각을 가진다
 */
public record AppointmentTime(AppointmentTimeType type, Instant scheduledAt) {

    private static final int MINUTE_STEP = 10;
    private static final int MAX_DAYS_AHEAD = 7;

    /**
     * 입력값으로 약속 시간을 만든다. 날짜 판단은 서비스 시간대 기준이므로 두 시각 모두 서비스 시간대 오프셋으로 받는다
     * @param type 시간 유형
     * @param scheduledAt 서비스 시간대의 약속 시각. NOW면 무시한다
     * @param now 서비스 시간대의 현재 시각
     */
    public static AppointmentTime of(AppointmentTimeType type, OffsetDateTime scheduledAt, OffsetDateTime now) {
        if (type == AppointmentTimeType.NOW) {
            // 지금 바로 약속은 시각을 받지 않는다. 보낸 값이 있어도 무시한다
            return new AppointmentTime(type, null);
        }
        validateScheduledAt(scheduledAt, now);
        return new AppointmentTime(type, scheduledAt.toInstant());
    }

    private static void validateScheduledAt(OffsetDateTime scheduledAt, OffsetDateTime now) {
        if (scheduledAt == null
                || !scheduledAt.isAfter(now)
                || scheduledAt.getMinute() % MINUTE_STEP != 0
                || scheduledAt.getSecond() != 0
                || scheduledAt.getNano() != 0
                || scheduledAt.toLocalDate().isAfter(now.toLocalDate().plusDays(MAX_DAYS_AHEAD))) {
            throw new ExchangeException(ExchangeErrorCode.INVALID_SCHEDULED_AT);
        }
    }
}
