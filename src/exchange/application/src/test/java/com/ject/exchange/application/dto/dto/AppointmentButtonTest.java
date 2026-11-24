package com.ject.exchange.application.dto.dto;

import com.ject.exchange.domain.domain.appointment.Appointment;
import com.ject.exchange.domain.domain.appointment.AppointmentStatus;
import com.ject.exchange.domain.domain.appointment.AppointmentTimeType;
import com.ject.exchange.domain.domain.appointment.vo.AppointmentItemIds;
import com.ject.exchange.domain.domain.appointment.vo.AppointmentTime;
import com.ject.exchange.domain.domain.appointment.vo.Place;
import com.ject.exchange.domain.domain.exchange.Exchange;
import com.ject.exchange.domain.domain.exchange.ExchangeStatus;
import com.ject.exchange.domain.domain.exchange.OfferType;
import com.ject.exchange.domain.domain.exchange.OfferedItem;
import com.ject.exchange.domain.domain.exchange.vo.ItemDescription;
import com.ject.exchange.domain.domain.exchange.vo.ItemName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 입력칸 + 메뉴의 약속 버튼 규칙 {@code AppointmentButton.of}를 상태 조합별로 확인한다 (#120).
 * 우선순위: 끝난 교환 NONE → 확정 약속(질문 전 CANCEL, 질문 뒤 NONE) → 게시글 묶임 · 없음 PROPOSE_DISABLED → PROPOSE.
 */
class AppointmentButtonTest {

    private static final Instant CREATED_AT = Instant.parse("2026-11-24T03:00:00Z");
    private static final Instant CONFIRMED_AT = Instant.parse("2026-11-24T04:00:00Z");
    private static final Instant SCHEDULED_AT = Instant.parse("2026-11-25T06:00:00Z");
    /** 약속 시각 + 30분 */
    private static final Instant SCHEDULED_OPENS_AT = SCHEDULED_AT.plus(Duration.ofMinutes(30));
    /** 지금 바로 약속: 확정 + 30분 */
    private static final Instant NOW_TYPE_OPENS_AT = CONFIRMED_AT.plus(Duration.ofMinutes(30));
    private static final Instant BEFORE_OPEN = Instant.parse("2026-11-24T05:00:00Z");
    private static final Long EXCHANGE_ID = 1L;
    private static final Long POST_ID = 10L;
    private static final Long REQUESTER_ID = 100L;
    private static final Long OWNER_ID = 200L;
    private static final Long ITEM_ID = 31L;
    private static final Place PLACE = new Place("홍대입구역 3번 출구 앞 편의점", "서울 마포구 양화로 160", 37.5571, 126.9245);

    // ---- AC-5 ----

    @Test
    @DisplayName("ACTIVE 교환 · 게시글 AVAILABLE · 확정 약속 없음이면 PROPOSE")
    void of_activeAvailableNoConfirmed_returnsPropose() {
        AppointmentButton button = AppointmentButton.of(exchange(ExchangeStatus.ACTIVE),
                post(PostInfo.Status.AVAILABLE), Optional.empty(), BEFORE_OPEN);

        assertEquals(AppointmentButton.PROPOSE, button);
    }

    // ---- AC-6 ----

    @Test
    @DisplayName("일정 확정 약속이면 교환 확인 열림 1초 전에 CANCEL")
    void of_scheduledConfirmedOneSecondBeforeOpen_returnsCancel() {
        AppointmentButton button = AppointmentButton.of(exchange(ExchangeStatus.ACTIVE),
                post(PostInfo.Status.RESERVED), Optional.of(scheduledConfirmed()),
                SCHEDULED_OPENS_AT.minusSeconds(1));

        assertEquals(AppointmentButton.CANCEL, button);
    }

    @Test
    @DisplayName("지금 바로 확정 약속이면 확정 + 30분 1초 전에 CANCEL")
    void of_nowTypeConfirmedOneSecondBeforeOpen_returnsCancel() {
        AppointmentButton button = AppointmentButton.of(exchange(ExchangeStatus.ACTIVE),
                post(PostInfo.Status.RESERVED), Optional.of(nowTypeConfirmed()),
                NOW_TYPE_OPENS_AT.minusSeconds(1));

        assertEquals(AppointmentButton.CANCEL, button);
    }

    @Test
    @DisplayName("확정 약속 때문에 게시글이 RESERVED여도 PROPOSE_DISABLED가 아니라 CANCEL")
    void of_confirmedWithReservedPost_cancelWinsOverDisabled() {
        AppointmentButton button = AppointmentButton.of(exchange(ExchangeStatus.ACTIVE),
                post(PostInfo.Status.RESERVED), Optional.of(scheduledConfirmed()), BEFORE_OPEN);

        assertEquals(AppointmentButton.CANCEL, button);
    }

    // ---- AC-7 ----

    @ParameterizedTest
    @ValueSource(longs = {0L, 86_400L})
    @DisplayName("일정 확정 약속이면 교환 확인 열림 시각과 그 하루 뒤에 NONE")
    void of_scheduledConfirmedAtOrAfterOpen_returnsNone(long secondsAfterOpen) {
        AppointmentButton button = AppointmentButton.of(exchange(ExchangeStatus.ACTIVE),
                post(PostInfo.Status.RESERVED), Optional.of(scheduledConfirmed()),
                SCHEDULED_OPENS_AT.plusSeconds(secondsAfterOpen));

        assertEquals(AppointmentButton.NONE, button);
    }

    @Test
    @DisplayName("지금 바로 확정 약속이면 확정 + 30분 정각에 NONE")
    void of_nowTypeConfirmedAtOpen_returnsNone() {
        AppointmentButton button = AppointmentButton.of(exchange(ExchangeStatus.ACTIVE),
                post(PostInfo.Status.RESERVED), Optional.of(nowTypeConfirmed()), NOW_TYPE_OPENS_AT);

        assertEquals(AppointmentButton.NONE, button);
    }

    @Test
    @DisplayName("교환 확인 질문 뒤에는 게시글이 삭제됐어도 PROPOSE_DISABLED가 아니라 NONE")
    void of_confirmedAfterOpenWithDeletedPost_returnsNone() {
        AppointmentButton button = AppointmentButton.of(exchange(ExchangeStatus.ACTIVE),
                post(PostInfo.Status.DELETED), Optional.of(scheduledConfirmed()), SCHEDULED_OPENS_AT);

        assertEquals(AppointmentButton.NONE, button);
    }

    // ---- AC-8 ----

    @ParameterizedTest
    @EnumSource(value = ExchangeStatus.class, names = {"COMPLETED", "CLOSED", "LEFT"})
    @DisplayName("끝난(COMPLETED · CLOSED · LEFT) 교환이면 게시글 AVAILABLE이어도 NONE")
    void of_endedExchange_returnsNone(ExchangeStatus status) {
        AppointmentButton button = AppointmentButton.of(exchange(status),
                post(PostInfo.Status.AVAILABLE), Optional.empty(), BEFORE_OPEN);

        assertEquals(AppointmentButton.NONE, button);
    }

    @ParameterizedTest
    @EnumSource(value = ExchangeStatus.class, names = {"COMPLETED", "CLOSED", "LEFT"})
    @DisplayName("끝난 교환이면 질문 전 확정 약속이 남아 있어도 CANCEL이 아니라 NONE")
    void of_endedExchangeWithConfirmed_returnsNone(ExchangeStatus status) {
        AppointmentButton button = AppointmentButton.of(exchange(status),
                post(PostInfo.Status.RESERVED), Optional.of(scheduledConfirmed()), BEFORE_OPEN);

        assertEquals(AppointmentButton.NONE, button);
    }

    // ---- AC-9 ----

    @ParameterizedTest
    @EnumSource(value = PostInfo.Status.class, names = {"RESERVED", "COMPLETED", "DELETED"})
    @DisplayName("확정 약속이 없고 게시글이 RESERVED · COMPLETED · DELETED면 PROPOSE_DISABLED")
    void of_postNotAvailable_returnsProposeDisabled(PostInfo.Status postStatus) {
        AppointmentButton button = AppointmentButton.of(exchange(ExchangeStatus.ACTIVE),
                post(postStatus), Optional.empty(), BEFORE_OPEN);

        assertEquals(AppointmentButton.PROPOSE_DISABLED, button);
    }

    @Test
    @DisplayName("게시글을 찾을 수 없으면 PROPOSE_DISABLED")
    void of_missingPost_returnsProposeDisabled() {
        AppointmentButton button = AppointmentButton.of(exchange(ExchangeStatus.ACTIVE),
                null, Optional.empty(), BEFORE_OPEN);

        assertEquals(AppointmentButton.PROPOSE_DISABLED, button);
    }

    private static Exchange exchange(ExchangeStatus status) {
        Instant completedAt = status == ExchangeStatus.COMPLETED ? CONFIRMED_AT : null;
        return Exchange.of(EXCHANGE_ID, POST_ID, REQUESTER_ID, OWNER_ID, OfferType.NEW,
                List.of(OfferedItem.of(ITEM_ID, null, new ItemName("치이카와 아크릴 스탠드"), false,
                        new ItemDescription("가챠에서 두 개 나와서 하나 교환해요"), 0)),
                status, completedAt, CREATED_AT, CREATED_AT);
    }

    private static PostInfo post(PostInfo.Status status) {
        return new PostInfo(POST_ID, "치이카와 교환해요", null, OWNER_ID, status);
    }

    private static Appointment scheduledConfirmed() {
        return confirmed(new AppointmentTime(AppointmentTimeType.SCHEDULED, SCHEDULED_AT));
    }

    private static Appointment nowTypeConfirmed() {
        return confirmed(new AppointmentTime(AppointmentTimeType.NOW, null));
    }

    private static Appointment confirmed(AppointmentTime time) {
        return Appointment.of(500L, EXCHANGE_ID, REQUESTER_ID, OWNER_ID, AppointmentStatus.CONFIRMED, time, PLACE,
                new AppointmentItemIds(List.of(ITEM_ID)), null, 30, 30, CONFIRMED_AT, null, List.of(),
                CREATED_AT, CREATED_AT);
    }
}
