package com.ject.exchange.api.dto.response;

import com.ject.common.web.image.ImageUrlResolver;
import com.ject.exchange.api.dto.enums.AppointmentStatus;
import com.ject.exchange.application.dto.dto.AppointmentItemResult;
import com.ject.exchange.application.dto.dto.AppointmentResult;
import com.ject.exchange.domain.domain.appointment.AppointmentTimeType;
import com.ject.exchange.domain.domain.appointment.vo.Place;
import com.ject.exchange.domain.domain.exchange.OfferType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 약속 응답의 필드 정리를 확인한다 (#117).
 */
class AppointmentResponseTest {

    private static final ImageUrlResolver IMAGE_URL_RESOLVER = new ImageUrlResolver("https://img.test");
    private static final Instant SCHEDULED_AT = Instant.parse("2026-10-27T06:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-10-26T05:00:00Z");

    @Test
    @DisplayName("약속 응답 필드는 pendingChange 없이 정해진 16개 순서다")
    void components_withoutPendingChange() {
        List<String> names = Arrays.stream(AppointmentResponse.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();

        assertEquals(List.of("id", "exchangeId", "proposerUserId", "receiverUserId", "status", "timeType",
                "scheduledAt", "place", "items", "expiresAt", "myReminderOffsetMinutes", "otherActiveProposalsOnPost",
                "canceledByUserId", "confirmation", "confirmedAt", "createdAt"), names);
    }

    @Test
    @DisplayName("약속 상태 응답 값에 EXPIRED가 없다")
    void status_hasNoExpired() {
        List<String> names = Arrays.stream(AppointmentStatus.values()).map(Enum::name).toList();

        assertEquals(List.of("PROPOSED", "REJECTED", "WITHDRAWN", "AUTO_REJECTED", "CONFIRMED", "COMPLETED",
                "NOT_MET", "CANCELED"), names);
    }

    @Test
    @DisplayName("조회 결과의 만료 시각 null · 내 알림 30이 응답에 그대로 담긴다")
    void from_scheduledProposal_mapsNullExpiryAndThirtyReminder() {
        AppointmentResponse response = AppointmentResponse.from(new AppointmentResult(3L, 1L, 100L, 200L,
                com.ject.exchange.domain.domain.appointment.AppointmentStatus.PROPOSED, AppointmentTimeType.SCHEDULED,
                SCHEDULED_AT, new Place("홍대입구역 3번 출구 앞 편의점", "서울 마포구 양화로 160", 37.5571, 126.9245),
                List.of(new AppointmentItemResult(31L, OfferType.POST, "우사기 미니 피규어", null)),
                null, 30, false, null, CREATED_AT), IMAGE_URL_RESOLVER);

        assertNull(response.expiresAt());
        assertEquals(30, response.myReminderOffsetMinutes());
        assertEquals(AppointmentStatus.PROPOSED, response.status());
    }
}
