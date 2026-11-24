package com.ject.exchange.application.service.query;

import com.ject.common.core.time.ServiceTime;
import com.ject.exchange.application.dto.dto.AppointmentResult;
import com.ject.exchange.application.dto.dto.ConfirmationSummary;
import com.ject.exchange.application.dto.dto.PostInfo;
import com.ject.exchange.application.dto.query.GetAppointmentQuery;
import com.ject.exchange.domain.domain.appointment.Appointment;
import com.ject.exchange.domain.domain.appointment.AppointmentStatus;
import com.ject.exchange.domain.domain.appointment.AppointmentTimeType;
import com.ject.exchange.domain.domain.appointment.ConfirmationAnswer;
import com.ject.exchange.domain.domain.appointment.NotMetReason;
import com.ject.exchange.domain.domain.appointment.vo.AppointmentItemIds;
import com.ject.exchange.domain.domain.appointment.vo.AppointmentTime;
import com.ject.exchange.domain.domain.appointment.vo.ConfirmationReply;
import com.ject.exchange.domain.domain.appointment.vo.Place;
import com.ject.exchange.domain.domain.exchange.Exchange;
import com.ject.exchange.domain.domain.exchange.ExchangeStatus;
import com.ject.exchange.domain.domain.exchange.OfferType;
import com.ject.exchange.domain.domain.exchange.OfferedItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
class GetAppointmentConfirmationTest {

    /** 2026-11-23 15:00 (+09:00) 약속 시각 */
    private static final Instant SCHEDULED_AT = Instant.parse("2026-11-23T06:00:00Z");
    /** 2026-11-23 15:30 (+09:00) 열림 시각 */
    private static final Instant OPENS_AT = SCHEDULED_AT.plus(Duration.ofMinutes(30));
    private static final Instant CONFIRMED_AT = Instant.parse("2026-11-22T08:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-11-22T07:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-11-22T06:00:00Z");
    /** A가 답한 시각 */
    private static final Instant RESOLVED_AT = OPENS_AT.plus(Duration.ofMinutes(10));
    private static final Long APPOINTMENT_ID = 3L;
    private static final Long EXCHANGE_ID = 1L;
    private static final Long POST_ID = 10L;
    private static final Long REQUESTER_ID = 100L;
    private static final Long OWNER_ID = 200L;
    private static final Long ITEM_31 = 31L;
    private static final Long SOURCE_POST_31 = 501L;
    private static final Place PLACE = new Place("홍대입구역 3번 출구 앞 편의점", "서울 마포구 양화로 160", 37.5571, 126.9245);

    private FakeExchangeRepository exchangeRepository;
    private FakeAppointmentRepository appointmentRepository;
    private FakePostPort postPort;

    @BeforeEach
    void setUp() {
        exchangeRepository = new FakeExchangeRepository();
        appointmentRepository = new FakeAppointmentRepository(exchangeRepository, CREATED_AT);
        postPort = new FakePostPort();
        postPort.register(new PostInfo(POST_ID, "치이카와 교환해요", null, OWNER_ID, PostInfo.Status.AVAILABLE));
        postPort.register(new PostInfo(SOURCE_POST_31, "우사기 미니 피규어", null, REQUESTER_ID,
                PostInfo.Status.AVAILABLE));
        exchangeRepository.preload(Exchange.of(EXCHANGE_ID, POST_ID, REQUESTER_ID, OWNER_ID, OfferType.POST,
                List.of(OfferedItem.of(ITEM_31, SOURCE_POST_31, null, null, null, 0)),
                ExchangeStatus.ACTIVE, null, CREATED_AT, CREATED_AT));
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class,
            names = {"PROPOSED", "REJECTED", "WITHDRAWN", "AUTO_REJECTED", "CANCELED"})
    @DisplayName("확정 전이거나 취소된 약속이면 confirmation은 null이다")
    void handle_notConfirmedOrCanceled_confirmationNull(AppointmentStatus status) {
        Instant confirmedAt = status == AppointmentStatus.CANCELED ? CONFIRMED_AT : null;
        appointmentRepository.preload(appointment(status, confirmedAt, null, List.of()));

        AppointmentResult result = handlerAt(OPENS_AT.plusSeconds(60)).handle(query(REQUESTER_ID));

        assertNull(result.confirmation());
    }

    @Test
    @DisplayName("CONFIRMED이고 열리기 전이면 askedAt · result · myAnswer · resolvedAt null · autoCompleted false다")
    void handle_confirmedBeforeOpen_allNull() {
        appointmentRepository.preload(appointment(AppointmentStatus.CONFIRMED, CONFIRMED_AT, null, List.of()));

        AppointmentResult result = handlerAt(OPENS_AT.minusSeconds(1)).handle(query(REQUESTER_ID));

        assertEquals(new ConfirmationSummary(null, null, false, null, null), result.confirmation());
    }

    @Test
    @DisplayName("CONFIRMED이고 열림 시각 정각이면 askedAt만 열림 시각이다")
    void handle_confirmedAtOpen_askedAtOnly() {
        appointmentRepository.preload(appointment(AppointmentStatus.CONFIRMED, CONFIRMED_AT, null, List.of()));

        AppointmentResult result = handlerAt(OPENS_AT).handle(query(OWNER_ID));

        assertEquals(new ConfirmationSummary(OPENS_AT, null, false, null, null), result.confirmation());
    }

    @Test
    @DisplayName("지금 바로 약속이 CONFIRMED이고 열린 뒤면 askedAt은 확정 시각 + 30분이다")
    void handle_nowAppointmentOpened_askedAtConfirmedPlus30() {
        appointmentRepository.preload(Appointment.of(APPOINTMENT_ID, EXCHANGE_ID, REQUESTER_ID, OWNER_ID,
                AppointmentStatus.CONFIRMED, new AppointmentTime(AppointmentTimeType.NOW, null), PLACE,
                new AppointmentItemIds(List.of(ITEM_31)), EXPIRES_AT, null, null, CONFIRMED_AT, null, List.of(),
                CREATED_AT, CREATED_AT));

        AppointmentResult result = handlerAt(CONFIRMED_AT.plus(Duration.ofHours(1))).handle(query(REQUESTER_ID));

        assertEquals(CONFIRMED_AT.plus(Duration.ofMinutes(30)), result.confirmation().askedAt());
    }

    @Test
    @DisplayName("COMPLETED이면 답한 A는 myAnswer DONE · B는 null이고 result COMPLETED · resolvedAt은 결과 시각이다")
    void handle_completed_perViewer() {
        appointmentRepository.preload(appointment(AppointmentStatus.COMPLETED, CONFIRMED_AT, RESOLVED_AT,
                List.of(new ConfirmationReply(REQUESTER_ID, ConfirmationAnswer.DONE, null, null, RESOLVED_AT))));
        GetAppointmentHandler handler = handlerAt(RESOLVED_AT.plusSeconds(60));

        assertEquals(new ConfirmationSummary(OPENS_AT, AppointmentStatus.COMPLETED, false, ConfirmationAnswer.DONE,
                RESOLVED_AT), handler.handle(query(REQUESTER_ID)).confirmation());
        assertEquals(new ConfirmationSummary(OPENS_AT, AppointmentStatus.COMPLETED, false, null, RESOLVED_AT),
                handler.handle(query(OWNER_ID)).confirmation());
    }

    @Test
    @DisplayName("NOT_MET이고 둘 다 답했으면 둘 다 myAnswer NOT_MET이고 resolvedAt은 먼저 답한 시각이다")
    void handle_notMetBothAnswered_perViewer() {
        appointmentRepository.preload(appointment(AppointmentStatus.NOT_MET, CONFIRMED_AT, RESOLVED_AT, List.of(
                new ConfirmationReply(REQUESTER_ID, ConfirmationAnswer.NOT_MET, NotMetReason.NO_SHOW, null,
                        RESOLVED_AT),
                new ConfirmationReply(OWNER_ID, ConfirmationAnswer.NOT_MET, NotMetReason.SELF_ABSENT, null,
                        RESOLVED_AT.plusSeconds(600)))));
        GetAppointmentHandler handler = handlerAt(RESOLVED_AT.plus(Duration.ofHours(1)));

        ConfirmationSummary expected = new ConfirmationSummary(OPENS_AT, AppointmentStatus.NOT_MET, false,
                ConfirmationAnswer.NOT_MET, RESOLVED_AT);
        assertEquals(expected, handler.handle(query(REQUESTER_ID)).confirmation());
        assertEquals(expected, handler.handle(query(OWNER_ID)).confirmation());
    }

    @Test
    @DisplayName("NOT_MET이고 상대가 아직 이유를 남기지 않았으면 상대의 myAnswer는 null이다")
    void handle_notMetOnlyFirst_otherMyAnswerNull() {
        appointmentRepository.preload(appointment(AppointmentStatus.NOT_MET, CONFIRMED_AT, RESOLVED_AT, List.of(
                new ConfirmationReply(OWNER_ID, ConfirmationAnswer.NOT_MET, NotMetReason.ITEM_MISMATCH, null,
                        RESOLVED_AT))));

        AppointmentResult result = handlerAt(RESOLVED_AT.plusSeconds(60)).handle(query(REQUESTER_ID));

        assertEquals(new ConfirmationSummary(OPENS_AT, AppointmentStatus.NOT_MET, false, null, RESOLVED_AT),
                result.confirmation());
    }

    private GetAppointmentHandler handlerAt(Instant now) {
        return new GetAppointmentHandler(appointmentRepository, exchangeRepository, postPort,
                new ServiceTime(Clock.fixed(now, ZoneId.of("Asia/Seoul"))));
    }

    private static GetAppointmentQuery query(Long viewerId) {
        return new GetAppointmentQuery(APPOINTMENT_ID, viewerId);
    }

    private static Appointment appointment(AppointmentStatus status, Instant confirmedAt, Instant resolvedAt,
                                           List<ConfirmationReply> replies) {
        return Appointment.of(APPOINTMENT_ID, EXCHANGE_ID, REQUESTER_ID, OWNER_ID, status,
                new AppointmentTime(AppointmentTimeType.SCHEDULED, SCHEDULED_AT), PLACE,
                new AppointmentItemIds(List.of(ITEM_31)), EXPIRES_AT, 30, null, confirmedAt, resolvedAt, replies,
                CREATED_AT, CREATED_AT);
    }
}
