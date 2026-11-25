package com.ject.exchange.application.service.command;

import com.ject.common.event.exchange.AppointmentCanceledByMemberWithdrawal;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("139-withdrawn-appointment-cancel-event")
class WithdrawnMemberAppointmentServiceTest {

    private static final Instant WITHDRAWN_AT = Instant.parse("2026-11-25T03:00:00Z");
    private static final Instant SCHEDULED_AT = Instant.parse("2026-11-25T01:00:00Z");
    private static final Instant CONFIRMED_AT = Instant.parse("2026-11-24T03:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-11-23T03:00:00Z");
    private static final Long WITHDRAWN_MEMBER_ID = 100L;
    private static final Long OTHER_MEMBER_ID = 200L;
    private static final Long THIRD_MEMBER_ID = 300L;
    private static final Long PROPOSED_APPOINTMENT_ID = 3L;
    private static final Long CONFIRMED_APPOINTMENT_ID = 4L;
    private static final Long FINISHED_APPOINTMENT_ID = 5L;
    private static final Long PROPOSED_EXCHANGE_ID = 11L;
    private static final Long CONFIRMED_EXCHANGE_ID = 12L;
    private static final Long FINISHED_EXCHANGE_ID = 13L;
    private static final Long PROPOSED_POST_ID = 21L;
    private static final Long PROPOSED_SOURCE_POST_ID = 31L;
    private static final Long CONFIRMED_POST_ID = 22L;
    private static final Long CONFIRMED_SOURCE_POST_ID = 32L;
    private static final Long FINISHED_POST_ID = 23L;
    private static final Long FINISHED_SOURCE_POST_ID = 33L;
    private static final Place PLACE = new Place(
            "홍대입구역 3번 출구 앞 편의점", "서울 마포구 양화로 160", 37.5571, 126.9245);

    private FakeExchangeRepository exchangeRepository;
    private FakeAppointmentRepository appointmentRepository;
    private FakePostLockRepository postLockRepository;
    private List<Object> publishedEvents;
    private WithdrawnMemberAppointmentService service;

    @BeforeEach
    void setUp() {
        exchangeRepository = new FakeExchangeRepository();
        appointmentRepository = new FakeAppointmentRepository(exchangeRepository, CREATED_AT);
        postLockRepository = new FakePostLockRepository(appointmentRepository);
        publishedEvents = new ArrayList<>();
        service = new WithdrawnMemberAppointmentService(
                appointmentRepository, postLockRepository, publishedEvents::add);
    }

    @Test
    @Tag("AC:139-withdrawn-appointment-cancel-event:AC-1")
    @DisplayName("탈퇴 회원의 진행 중 약속이 없으면 저장·잠금·이벤트가 없다")
    void cancelActiveAppointments_withoutActiveAppointment_doesNothing() {
        service.cancelActiveAppointments(WITHDRAWN_MEMBER_ID, WITHDRAWN_AT);

        assertEquals(0, appointmentRepository.saveCount());
        assertTrue(postLockRepository.lockedPostIds().isEmpty());
        assertTrue(publishedEvents.isEmpty());
    }

    @Test
    @Tag("AC:139-withdrawn-appointment-cancel-event:AC-2")
    @Tag("AC:139-withdrawn-appointment-cancel-event:AC-3")
    @Tag("AC:139-withdrawn-appointment-cancel-event:AC-5")
    @Tag("AC:139-withdrawn-appointment-cancel-event:AC-6")
    @Tag("AC:139-withdrawn-appointment-cancel-event:AC-9")
    @DisplayName("제안자인 PROPOSED와 받는 사람인 CONFIRMED를 취소하고 확정 약속 이벤트만 발행한다")
    void cancelActiveAppointments_mixedStatuses_cancelsAndPublishesConfirmedOnly() {
        preload(PROPOSED_EXCHANGE_ID, PROPOSED_POST_ID, PROPOSED_SOURCE_POST_ID,
                PROPOSED_APPOINTMENT_ID, AppointmentStatus.PROPOSED,
                WITHDRAWN_MEMBER_ID, OTHER_MEMBER_ID);
        preload(CONFIRMED_EXCHANGE_ID, CONFIRMED_POST_ID, CONFIRMED_SOURCE_POST_ID,
                CONFIRMED_APPOINTMENT_ID, AppointmentStatus.CONFIRMED,
                THIRD_MEMBER_ID, WITHDRAWN_MEMBER_ID);

        service.cancelActiveAppointments(WITHDRAWN_MEMBER_ID, WITHDRAWN_AT);

        assertEquals(AppointmentStatus.CANCELED, stored(PROPOSED_APPOINTMENT_ID).getStatus());
        assertEquals(AppointmentStatus.CANCELED, stored(CONFIRMED_APPOINTMENT_ID).getStatus());
        assertEquals(WITHDRAWN_MEMBER_ID, stored(PROPOSED_APPOINTMENT_ID).getCanceledByUserId());
        assertEquals(WITHDRAWN_MEMBER_ID, stored(CONFIRMED_APPOINTMENT_ID).getCanceledByUserId());
        assertEquals(2, appointmentRepository.saveCount());
        assertEquals(List.of(new AppointmentCanceledByMemberWithdrawal(
                CONFIRMED_APPOINTMENT_ID,
                List.of(CONFIRMED_POST_ID, CONFIRMED_SOURCE_POST_ID),
                WITHDRAWN_MEMBER_ID)), publishedEvents);
        assertEquals(List.of(
                List.of(PROPOSED_POST_ID, PROPOSED_SOURCE_POST_ID),
                List.of(CONFIRMED_POST_ID, CONFIRMED_SOURCE_POST_ID)),
                postLockRepository.lockedPostIds());
        assertEquals(List.of(0, 1), postLockRepository.lockedLookupCountsAtLock());
    }

    @Test
    @Tag("AC:139-withdrawn-appointment-cancel-event:AC-5")
    @DisplayName("확정 약속 이벤트에는 선택하지 않은 첨부 글을 넣지 않는다")
    void cancelActiveAppointments_confirmed_includesOnlyChosenSourcePost() {
        Exchange exchange = Exchange.of(CONFIRMED_EXCHANGE_ID, CONFIRMED_POST_ID,
                WITHDRAWN_MEMBER_ID, OTHER_MEMBER_ID, OfferType.POST,
                List.of(OfferedItem.of(41L, CONFIRMED_SOURCE_POST_ID, null, null, null, 0),
                        OfferedItem.of(42L, 99L, null, null, null, 1)),
                ExchangeStatus.ACTIVE, null, CREATED_AT, CREATED_AT);
        exchangeRepository.preload(exchange);
        appointmentRepository.preload(appointment(CONFIRMED_APPOINTMENT_ID, CONFIRMED_EXCHANGE_ID,
                AppointmentStatus.CONFIRMED, WITHDRAWN_MEMBER_ID, OTHER_MEMBER_ID, 41L));

        service.cancelActiveAppointments(WITHDRAWN_MEMBER_ID, WITHDRAWN_AT);

        AppointmentCanceledByMemberWithdrawal event =
                (AppointmentCanceledByMemberWithdrawal) publishedEvents.get(0);
        assertEquals(List.of(CONFIRMED_POST_ID, CONFIRMED_SOURCE_POST_ID), event.postIds());
    }

    @Test
    @Tag("AC:139-withdrawn-appointment-cancel-event:AC-8")
    @DisplayName("후보 조회 뒤 끝난 약속은 잠금 뒤 다시 취소하거나 이벤트를 발행하지 않는다")
    void cancelActiveAppointments_statusChangedAfterCandidateQuery_skips() {
        preload(FINISHED_EXCHANGE_ID, FINISHED_POST_ID, FINISHED_SOURCE_POST_ID,
                FINISHED_APPOINTMENT_ID, AppointmentStatus.CANCELED,
                WITHDRAWN_MEMBER_ID, OTHER_MEMBER_ID);
        FakeAppointmentRepository staleCandidateRepository = new FakeAppointmentRepository(
                exchangeRepository, CREATED_AT) {
            @Override
            public List<Long> findIdsByMemberIdAndStatuses(
                    Long memberId, java.util.Collection<AppointmentStatus> statuses) {
                return List.of(FINISHED_APPOINTMENT_ID);
            }
        };
        staleCandidateRepository.preload(appointment(FINISHED_APPOINTMENT_ID, FINISHED_EXCHANGE_ID,
                AppointmentStatus.CANCELED, WITHDRAWN_MEMBER_ID, OTHER_MEMBER_ID, 41L));
        FakePostLockRepository staleCandidateLockRepository =
                new FakePostLockRepository(staleCandidateRepository);
        List<Object> events = new ArrayList<>();
        WithdrawnMemberAppointmentService staleCandidateService = new WithdrawnMemberAppointmentService(
                staleCandidateRepository, staleCandidateLockRepository, events::add);

        staleCandidateService.cancelActiveAppointments(WITHDRAWN_MEMBER_ID, WITHDRAWN_AT);

        assertEquals(0, staleCandidateRepository.saveCount());
        assertTrue(events.isEmpty());
        assertEquals(List.of(List.of(FINISHED_POST_ID, FINISHED_SOURCE_POST_ID)),
                staleCandidateLockRepository.lockedPostIds());
    }

    private void preload(Long exchangeId, Long postId, Long sourcePostId, Long appointmentId,
                         AppointmentStatus status, Long proposerId, Long receiverId) {
        exchangeRepository.preload(Exchange.of(exchangeId, postId, proposerId, receiverId, OfferType.POST,
                List.of(OfferedItem.of(41L, sourcePostId, null, null, null, 0)),
                ExchangeStatus.ACTIVE, null, CREATED_AT, CREATED_AT));
        appointmentRepository.preload(appointment(
                appointmentId, exchangeId, status, proposerId, receiverId, 41L));
    }

    private Appointment stored(Long appointmentId) {
        return appointmentRepository.findById(appointmentId).orElseThrow();
    }

    private static Appointment appointment(Long appointmentId, Long exchangeId, AppointmentStatus status,
                                           Long proposerId, Long receiverId, Long itemId) {
        return Appointment.of(appointmentId, exchangeId, proposerId, receiverId, status,
                new AppointmentTime(AppointmentTimeType.SCHEDULED, SCHEDULED_AT), PLACE,
                new AppointmentItemIds(List.of(itemId)), null, 30, 60,
                status == AppointmentStatus.CONFIRMED ? CONFIRMED_AT : null,
                null, List.of(), CREATED_AT, CREATED_AT, null, null);
    }
}
