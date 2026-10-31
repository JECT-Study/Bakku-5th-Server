package com.ject.exchange.api.controller;

import com.ject.exchange.api.dto.enums.AppointmentStatus;
import com.ject.exchange.api.dto.enums.AppointmentTimeType;
import com.ject.exchange.api.dto.enums.OfferType;
import com.ject.exchange.api.dto.response.AppointmentChangeResponse;
import com.ject.exchange.api.dto.response.AppointmentItemResponse;
import com.ject.exchange.api.dto.response.AppointmentResponse;
import com.ject.exchange.api.dto.response.ConfirmationResponse;
import com.ject.exchange.api.dto.response.PlaceResponse;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

final class ExchangeStubData {

    private static final ZoneOffset KST = ZoneOffset.ofHours(9);

    static final Long EXCHANGE_ID = 1L;
    static final Long APPOINTMENT_ID = 3L;
    static final Long CHAT_ROOM_ID = 5L;
    static final Long CANCELLATION_ID = 7L;
    static final Long CHANGE_ID = 9L;
    static final Long POST_ID = 10L;
    static final Long REQUESTER_ID = 101L;
    static final Long OWNER_ID = 202L;

    static final OffsetDateTime EXCHANGE_CREATED_AT = at(26, 14, 30);
    static final OffsetDateTime APPOINTMENT_CREATED_AT = at(26, 16, 40);
    static final OffsetDateTime APPOINTMENT_CONFIRMED_AT = at(26, 17, 0);
    static final OffsetDateTime SCHEDULED_AT = at(27, 15, 0);
    static final OffsetDateTime CHANGED_SCHEDULED_AT = at(27, 16, 0);
    static final OffsetDateTime CHANGE_REQUESTED_AT = at(27, 10, 0);

    static final List<Long> CHANGE_OFFERED_ITEM_IDS = List.of(31L, 35L);

    private ExchangeStubData() {
    }

    static PlaceResponse place() {
        return new PlaceResponse("홍대입구역 3번 출구 앞 편의점", "서울 마포구 양화로 160", 37.5571, 126.9245);
    }

    static List<AppointmentItemResponse> appointmentItems() {
        return List.of(
                new AppointmentItemResponse(31L, OfferType.POST, "우사기 미니 피규어", "https://{cdn}/post/21/1.jpg"),
                new AppointmentItemResponse(35L, OfferType.POST, "치이카와 랜덤 뱃지", null));
    }

    static AppointmentResponse confirmedAppointment(Long appointmentId, Long exchangeId, AppointmentStatus status,
                                                    OffsetDateTime scheduledAt, Integer myReminderOffsetMinutes,
                                                    AppointmentChangeResponse pendingChange,
                                                    ConfirmationResponse confirmation) {
        return new AppointmentResponse(
                appointmentId, exchangeId, REQUESTER_ID, OWNER_ID,
                status, AppointmentTimeType.SCHEDULED, scheduledAt, place(), appointmentItems(),
                null, myReminderOffsetMinutes, false,
                pendingChange, null, confirmation,
                APPOINTMENT_CONFIRMED_AT, APPOINTMENT_CREATED_AT);
    }

    static OffsetDateTime at(int dayOfOctober, int hour, int minute) {
        return OffsetDateTime.of(2026, 10, dayOfOctober, hour, minute, 0, 0, KST);
    }
}
