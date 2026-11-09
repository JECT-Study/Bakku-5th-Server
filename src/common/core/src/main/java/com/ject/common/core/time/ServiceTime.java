package com.ject.common.core.time;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * 서비스 시간대 기준의 현재 시각 · 날짜 · 응답용 변환을 한곳에서 제공한다.
 * 시각은 Instant로 저장 · 계산하고, 날짜 판단과 API 표현에서만 서비스 시간대를 적용한다 (결정 A6-1 · A22).
 */
public final class ServiceTime {

    private final Clock clock;

    public ServiceTime(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    public Instant now() {
        return clock.instant();
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /**
     * @param instant null 가능 (탈퇴 시각처럼 비어 있을 수 있는 값)
     * @return 서비스 시간대의 오프셋을 붙인 값, instant가 null이면 null
     */
    public OffsetDateTime toOffset(Instant instant) {
        return instant == null ? null : instant.atZone(clock.getZone()).toOffsetDateTime();
    }
}
