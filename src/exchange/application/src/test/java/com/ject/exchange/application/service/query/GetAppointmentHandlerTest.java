package com.ject.exchange.application.service.query;

import com.ject.common.core.time.ServiceTime;
import com.ject.exchange.application.dto.dto.AppointmentItemResult;
import com.ject.exchange.application.dto.dto.AppointmentResult;
import com.ject.exchange.application.dto.dto.PostInfo;
import com.ject.exchange.application.dto.query.GetAppointmentQuery;
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
import com.ject.exchange.domain.exception.ExchangeErrorCode;
import com.ject.exchange.domain.exception.ExchangeException;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
class GetAppointmentHandlerTest {

    /** 2026-10-26 14:00 (+09:00) */
    private static final Instant NOW = Instant.parse("2026-10-26T05:00:00Z");
    private static final Instant SCHEDULED_AT = Instant.parse("2026-10-27T06:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-10-27T05:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-10-26T04:50:00Z");
    private static final Long APPOINTMENT_ID = 3L;
    private static final Long OTHER_APPOINTMENT_ID = 4L;
    private static final Long MISSING_APPOINTMENT_ID = 999L;
    private static final Long EXCHANGE_ID = 1L;
    private static final Long OTHER_EXCHANGE_ID = 2L;
    private static final Long POST_ID = 10L;
    private static final Long OTHER_POST_ID = 11L;
    private static final Long REQUESTER_ID = 100L;
    private static final Long OWNER_ID = 200L;
    private static final Long OTHER_MEMBER_ID = 300L;
    private static final Long ITEM_31 = 31L;
    private static final Long ITEM_32 = 32L;
    private static final Long SOURCE_POST_31 = 501L;
    private static final Long SOURCE_POST_32 = 502L;
    private static final String THUMBNAIL_31 = "https://cdn.example.com/post/501/1.jpg";
    private static final Place PLACE = new Place("홍대입구역 3번 출구 앞 편의점", "서울 마포구 양화로 160", 37.5571, 126.9245);

    private FakeExchangeRepository exchangeRepository;
    private FakeAppointmentRepository appointmentRepository;
    private FakePostPort postPort;
    private GetAppointmentHandler handler;

    @BeforeEach
    void setUp() {
        exchangeRepository = new FakeExchangeRepository();
        appointmentRepository = new FakeAppointmentRepository(exchangeRepository, NOW);
        postPort = new FakePostPort();
        handler = new GetAppointmentHandler(appointmentRepository, exchangeRepository, postPort,
                new ServiceTime(Clock.fixed(NOW, ZoneId.of("Asia/Seoul"))));

        postPort.register(new PostInfo(POST_ID, "치이카와 교환해요", null, OWNER_ID, PostInfo.Status.AVAILABLE));
        postPort.register(new PostInfo(SOURCE_POST_31, "우사기 미니 피규어", THUMBNAIL_31, REQUESTER_ID,
                PostInfo.Status.AVAILABLE));
        postPort.register(new PostInfo(SOURCE_POST_32, "하치와레 키링", null, REQUESTER_ID,
                PostInfo.Status.AVAILABLE));
    }

    @Test
    @DisplayName("제안자가 조회하면 약속 값 · 아이템 · 만료 시각 · 내 알림 값을 담아 반환한다")
    void handle_byProposer_returnsAppointmentResult() {
        // given
        exchangeRepository.preload(postItemExchange(EXCHANGE_ID, POST_ID, REQUESTER_ID));
        appointmentRepository.preload(appointment(APPOINTMENT_ID, EXCHANGE_ID, AppointmentStatus.PROPOSED,
                EXPIRES_AT, List.of(ITEM_31)));

        // when
        AppointmentResult result = handler.handle(new GetAppointmentQuery(APPOINTMENT_ID, REQUESTER_ID));

        // then
        assertEquals(APPOINTMENT_ID, result.id());
        assertEquals(EXCHANGE_ID, result.exchangeId());
        assertEquals(REQUESTER_ID, result.proposerUserId());
        assertEquals(OWNER_ID, result.receiverUserId());
        assertEquals(AppointmentStatus.PROPOSED, result.status());
        assertEquals(AppointmentTimeType.SCHEDULED, result.timeType());
        assertEquals(SCHEDULED_AT, result.scheduledAt());
        assertEquals(PLACE, result.place());
        assertEquals(List.of(new AppointmentItemResult(ITEM_31, OfferType.POST, "우사기 미니 피규어", THUMBNAIL_31)),
                result.items());
        assertEquals(EXPIRES_AT, result.expiresAt());
        assertEquals(30, result.myReminderOffsetMinutes());
        assertFalse(result.otherActiveProposalsOnPost());
        assertNull(result.confirmedAt());
        assertEquals(CREATED_AT, result.createdAt());
    }

    @Test
    @DisplayName("고른 POST 아이템 2개는 고른 순서대로 원본 글 제목을 이름으로 담는다")
    void handle_twoPostItems_returnsTitles() {
        exchangeRepository.preload(postItemExchange(EXCHANGE_ID, POST_ID, REQUESTER_ID));
        appointmentRepository.preload(appointment(APPOINTMENT_ID, EXCHANGE_ID, AppointmentStatus.PROPOSED,
                EXPIRES_AT, List.of(ITEM_31, ITEM_32)));

        AppointmentResult result = handler.handle(new GetAppointmentQuery(APPOINTMENT_ID, REQUESTER_ID));

        assertEquals(List.of(
                new AppointmentItemResult(ITEM_31, OfferType.POST, "우사기 미니 피규어", THUMBNAIL_31),
                new AppointmentItemResult(ITEM_32, OfferType.POST, "하치와레 키링", null)), result.items());
    }

    @Test
    @DisplayName("NEW 아이템은 제품명을 이름으로 담고 사진은 null이다")
    void handle_newItem_returnsProductName() {
        exchangeRepository.preload(Exchange.of(EXCHANGE_ID, POST_ID, REQUESTER_ID, OWNER_ID, OfferType.NEW,
                List.of(OfferedItem.of(ITEM_31, null, new ItemName("치이카와 아크릴 스탠드"), false,
                        new ItemDescription("가챠에서 두 개 나와서 하나 교환해요"), 0)),
                ExchangeStatus.ACTIVE, null, NOW, NOW));
        appointmentRepository.preload(appointment(APPOINTMENT_ID, EXCHANGE_ID, AppointmentStatus.PROPOSED,
                EXPIRES_AT, List.of(ITEM_31)));

        AppointmentResult result = handler.handle(new GetAppointmentQuery(APPOINTMENT_ID, REQUESTER_ID));

        assertEquals(List.of(new AppointmentItemResult(ITEM_31, OfferType.NEW, "치이카와 아크릴 스탠드", null)),
                result.items());
    }

    @Test
    @DisplayName("받는 사람이 조회하면 내 알림 값은 받는 사람 값(null)이다")
    void handle_byReceiver_returnsReceiverReminder() {
        exchangeRepository.preload(postItemExchange(EXCHANGE_ID, POST_ID, REQUESTER_ID));
        appointmentRepository.preload(appointment(APPOINTMENT_ID, EXCHANGE_ID, AppointmentStatus.PROPOSED,
                EXPIRES_AT, List.of(ITEM_31)));

        AppointmentResult result = handler.handle(new GetAppointmentQuery(APPOINTMENT_ID, OWNER_ID));

        assertNull(result.myReminderOffsetMinutes());
    }

    @Test
    @DisplayName("PROPOSED가 아니면 만료 시각은 null이다")
    void handle_confirmed_hasNullExpiresAt() {
        exchangeRepository.preload(postItemExchange(EXCHANGE_ID, POST_ID, REQUESTER_ID));
        appointmentRepository.preload(appointment(APPOINTMENT_ID, EXCHANGE_ID, AppointmentStatus.CONFIRMED,
                EXPIRES_AT, List.of(ITEM_31)));

        AppointmentResult result = handler.handle(new GetAppointmentQuery(APPOINTMENT_ID, REQUESTER_ID));

        assertEquals(AppointmentStatus.CONFIRMED, result.status());
        assertNull(result.expiresAt());
    }

    @Test
    @DisplayName("없는 약속이면 APPOINTMENT_NOT_FOUND")
    void handle_missingAppointment_throwsAppointmentNotFound() {
        exchangeRepository.preload(postItemExchange(EXCHANGE_ID, POST_ID, REQUESTER_ID));

        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> handler.handle(new GetAppointmentQuery(MISSING_APPOINTMENT_ID, REQUESTER_ID)));

        assertEquals(ExchangeErrorCode.APPOINTMENT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("참여자가 아닌 회원이 조회하면 NOT_EXCHANGE_PARTICIPANT")
    void handle_thirdParty_throwsNotExchangeParticipant() {
        exchangeRepository.preload(postItemExchange(EXCHANGE_ID, POST_ID, REQUESTER_ID));
        appointmentRepository.preload(appointment(APPOINTMENT_ID, EXCHANGE_ID, AppointmentStatus.PROPOSED,
                EXPIRES_AT, List.of(ITEM_31)));

        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> handler.handle(new GetAppointmentQuery(APPOINTMENT_ID, OTHER_MEMBER_ID)));

        assertEquals(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT, exception.getErrorCode());
    }

    // ---- 다른 방 진행 중 제안 ----

    @Test
    @DisplayName("같은 글의 다른 교환에 만료 전 PROPOSED가 있으면 otherActiveProposalsOnPost가 true다")
    void handle_pendingProposalInOtherExchange_returnsTrue() {
        givenAppointmentAndOtherExchange(POST_ID);
        appointmentRepository.preload(appointment(OTHER_APPOINTMENT_ID, OTHER_EXCHANGE_ID,
                AppointmentStatus.PROPOSED, NOW.plus(Duration.ofHours(1)), List.of(41L)));

        AppointmentResult result = handler.handle(new GetAppointmentQuery(APPOINTMENT_ID, REQUESTER_ID));

        assertTrue(result.otherActiveProposalsOnPost());
    }

    @Test
    @DisplayName("다른 교환에 약속이 없으면 otherActiveProposalsOnPost가 false다")
    void handle_noOtherProposal_returnsFalse() {
        givenAppointmentAndOtherExchange(POST_ID);

        AppointmentResult result = handler.handle(new GetAppointmentQuery(APPOINTMENT_ID, REQUESTER_ID));

        assertFalse(result.otherActiveProposalsOnPost());
    }

    @Test
    @DisplayName("다른 교환의 PROPOSED가 만료 시각을 지났으면 otherActiveProposalsOnPost가 false다")
    void handle_expiredProposalInOtherExchange_returnsFalse() {
        givenAppointmentAndOtherExchange(POST_ID);
        appointmentRepository.preload(appointment(OTHER_APPOINTMENT_ID, OTHER_EXCHANGE_ID,
                AppointmentStatus.PROPOSED, NOW.minus(Duration.ofMinutes(10)), List.of(41L)));

        AppointmentResult result = handler.handle(new GetAppointmentQuery(APPOINTMENT_ID, REQUESTER_ID));

        assertFalse(result.otherActiveProposalsOnPost());
    }

    @ParameterizedTest
    @EnumSource(value = AppointmentStatus.class, names = {"CONFIRMED", "REJECTED", "WITHDRAWN", "EXPIRED"})
    @DisplayName("다른 교환의 약속이 PROPOSED가 아니면 otherActiveProposalsOnPost가 false다")
    void handle_notProposedInOtherExchange_returnsFalse(AppointmentStatus status) {
        givenAppointmentAndOtherExchange(POST_ID);
        appointmentRepository.preload(appointment(OTHER_APPOINTMENT_ID, OTHER_EXCHANGE_ID, status,
                NOW.plus(Duration.ofHours(1)), List.of(41L)));

        AppointmentResult result = handler.handle(new GetAppointmentQuery(APPOINTMENT_ID, REQUESTER_ID));

        assertFalse(result.otherActiveProposalsOnPost());
    }

    @Test
    @DisplayName("다른 글의 교환에 진행 중 제안이 있으면 otherActiveProposalsOnPost가 false다")
    void handle_pendingProposalOnOtherPost_returnsFalse() {
        givenAppointmentAndOtherExchange(OTHER_POST_ID);
        appointmentRepository.preload(appointment(OTHER_APPOINTMENT_ID, OTHER_EXCHANGE_ID,
                AppointmentStatus.PROPOSED, NOW.plus(Duration.ofHours(1)), List.of(41L)));

        AppointmentResult result = handler.handle(new GetAppointmentQuery(APPOINTMENT_ID, REQUESTER_ID));

        assertFalse(result.otherActiveProposalsOnPost());
    }

    // ---- helpers ----

    private void givenAppointmentAndOtherExchange(Long otherExchangePostId) {
        exchangeRepository.preload(postItemExchange(EXCHANGE_ID, POST_ID, REQUESTER_ID));
        exchangeRepository.preload(Exchange.of(OTHER_EXCHANGE_ID, otherExchangePostId, OTHER_MEMBER_ID, OWNER_ID,
                OfferType.POST, List.of(OfferedItem.of(41L, 601L, null, null, null, 0)),
                ExchangeStatus.ACTIVE, null, NOW, NOW));
        appointmentRepository.preload(appointment(APPOINTMENT_ID, EXCHANGE_ID, AppointmentStatus.PROPOSED,
                EXPIRES_AT, List.of(ITEM_31)));
    }

    private static Exchange postItemExchange(Long exchangeId, Long postId, Long requesterId) {
        return Exchange.of(exchangeId, postId, requesterId, OWNER_ID, OfferType.POST, List.of(
                        OfferedItem.of(ITEM_31, SOURCE_POST_31, null, null, null, 0),
                        OfferedItem.of(ITEM_32, SOURCE_POST_32, null, null, null, 1)),
                ExchangeStatus.ACTIVE, null, NOW, NOW);
    }

    /** 이 교환의 약속은 요청자가 제안했고, 다른 교환의 약속은 그 교환의 요청자가 제안했다. */
    private static Appointment appointment(Long id, Long exchangeId, AppointmentStatus status, Instant expiresAt,
                                           List<Long> itemIds) {
        Long proposerId = exchangeId.equals(EXCHANGE_ID) ? REQUESTER_ID : OTHER_MEMBER_ID;
        Instant confirmedAt = status == AppointmentStatus.CONFIRMED ? NOW : null;
        return Appointment.of(id, exchangeId, proposerId, OWNER_ID, status,
                new AppointmentTime(AppointmentTimeType.SCHEDULED, SCHEDULED_AT), PLACE,
                new AppointmentItemIds(itemIds), expiresAt, 30, null, confirmedAt, CREATED_AT, CREATED_AT);
    }
}
