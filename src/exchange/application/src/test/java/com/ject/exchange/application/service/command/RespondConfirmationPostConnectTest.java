package com.ject.exchange.application.service.command;

import com.ject.common.core.time.ServiceTime;
import com.ject.exchange.application.dto.command.RespondConfirmationCommand;
import com.ject.exchange.application.dto.dto.PostInfo;
import com.ject.exchange.application.port.PostStatusRejectedException;
import com.ject.exchange.domain.domain.appointment.Appointment;
import com.ject.exchange.domain.domain.appointment.AppointmentStatus;
import com.ject.exchange.domain.domain.appointment.AppointmentTimeType;
import com.ject.exchange.domain.domain.appointment.ConfirmationAnswer;
import com.ject.exchange.domain.domain.appointment.NotMetReason;
import com.ject.exchange.domain.domain.appointment.vo.AppointmentItemIds;
import com.ject.exchange.domain.domain.appointment.vo.AppointmentTime;
import com.ject.exchange.domain.domain.appointment.vo.Place;
import com.ject.exchange.domain.domain.exchange.Exchange;
import com.ject.exchange.domain.domain.exchange.ExchangeStatus;
import com.ject.exchange.domain.domain.exchange.OfferType;
import com.ject.exchange.domain.domain.exchange.OfferedItem;
import com.ject.exchange.domain.exception.ExchangeErrorCode;
import com.ject.exchange.domain.exception.ExchangeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * F-1 교환 확인이 post BC 글 상태를 완료 · 해제로 바꾸고, 거부는 잡지 않고 전파하는지 확인한다 (#107).
 * 롤백은 트랜잭션이 하므로 Fake로는 전파 여부와 post 요청만 본다.
 */
class RespondConfirmationPostConnectTest {

    /** 2026-11-23 15:00 (+09:00) 약속 시각 */
    private static final Instant SCHEDULED_AT = Instant.parse("2026-11-23T06:00:00Z");
    /** 교환 확인 질문이 열린 시각 */
    private static final Instant NOW = SCHEDULED_AT.plus(Duration.ofMinutes(30));
    private static final Instant CONFIRMED_AT = Instant.parse("2026-11-22T08:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-11-22T07:00:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-11-22T06:00:00Z");
    private static final Long APPOINTMENT_ID = 3L;
    private static final Long EXCHANGE_X = 1L;
    private static final Long POST_P = 10L;
    private static final Long POST_Q1 = 20L;
    private static final Long POST_Q2 = 21L;
    private static final Long MEMBER_A = 101L;
    private static final Long MEMBER_B = 102L;
    private static final Long ITEM_Q1 = 31L;
    private static final Long ITEM_Q2 = 32L;
    private static final Place PLACE = new Place("홍대입구역 3번 출구 앞 편의점", "서울 마포구 양화로 160", 37.5571, 126.9245);

    private FakeExchangeRepository exchangeRepository;
    private FakeAppointmentRepository appointmentRepository;
    private FakePostLockRepository postLockRepository;
    private FakePostPort postPort;
    private List<Object> publishedEvents;

    @BeforeEach
    void setUp() {
        exchangeRepository = new FakeExchangeRepository();
        appointmentRepository = new FakeAppointmentRepository(exchangeRepository, CREATED_AT);
        postLockRepository = new FakePostLockRepository(appointmentRepository);
        postPort = new FakePostPort();
        publishedEvents = new ArrayList<>();

        // AC-9의 확정 약속: P · Q1 예약중, Q2 교환 가능
        postPort.register(post(POST_P, MEMBER_B, PostInfo.Status.RESERVED));
        postPort.register(post(POST_Q1, MEMBER_A, PostInfo.Status.RESERVED));
        postPort.register(post(POST_Q2, MEMBER_A, PostInfo.Status.AVAILABLE));
        exchangeRepository.preload(Exchange.of(EXCHANGE_X, POST_P, MEMBER_A, MEMBER_B, OfferType.POST,
                List.of(OfferedItem.of(ITEM_Q1, POST_Q1, null, null, null, 0),
                        OfferedItem.of(ITEM_Q2, POST_Q2, null, null, null, 1)),
                ExchangeStatus.ACTIVE, null, CREATED_AT, CREATED_AT));
        appointmentRepository.preload(Appointment.of(APPOINTMENT_ID, EXCHANGE_X, MEMBER_A, MEMBER_B,
                AppointmentStatus.CONFIRMED, new AppointmentTime(AppointmentTimeType.SCHEDULED, SCHEDULED_AT),
                PLACE, new AppointmentItemIds(List.of(ITEM_Q1)), EXPIRES_AT, 30, 60, CONFIRMED_AT,
                null, List.of(), CREATED_AT, CREATED_AT));
    }

    // ---- AC-13 ----

    @Test
    @DisplayName("DONE이면 P · Q1이 교환 완료이고 Q2는 교환 가능 그대로다")
    void handle_done_completesPostAndChosenAttachedPost() {
        // when
        handlerAt(NOW).handle(done(MEMBER_A));

        // then
        assertEquals(1, postPort.completeRequests().size());
        assertEquals(Set.of(POST_P, POST_Q1), Set.copyOf(postPort.completeRequests().get(0)));
        assertEquals(PostInfo.Status.COMPLETED, postPort.statusOf(POST_P));
        assertEquals(PostInfo.Status.COMPLETED, postPort.statusOf(POST_Q1));
        assertEquals(PostInfo.Status.AVAILABLE, postPort.statusOf(POST_Q2));
        assertTrue(postPort.releaseRequests().isEmpty());
    }

    // ---- AC-14 ----

    @Test
    @DisplayName("NOT_MET이면 P · Q1이 교환 가능으로 돌아간다")
    void handle_notMet_releasesPostAndChosenAttachedPost() {
        // when
        handlerAt(NOW).handle(notMet(MEMBER_A));

        // then
        assertEquals(1, postPort.releaseRequests().size());
        assertEquals(Set.of(POST_P, POST_Q1), Set.copyOf(postPort.releaseRequests().get(0)));
        assertEquals(PostInfo.Status.AVAILABLE, postPort.statusOf(POST_P));
        assertEquals(PostInfo.Status.AVAILABLE, postPort.statusOf(POST_Q1));
        assertTrue(postPort.completeRequests().isEmpty());
    }

    // ---- AC-15 ----

    @Test
    @DisplayName("DONE 뒤 B가 NOT_MET으로 답해도 해제 요청 없이 P · Q1은 교환 완료 그대로다")
    void handle_notMetAfterDone_keepsCompleted() {
        // given
        handlerAt(NOW).handle(done(MEMBER_A));

        // when
        handlerAt(NOW.plusSeconds(60)).handle(notMet(MEMBER_B));

        // then
        assertEquals(1, postPort.completeRequests().size());
        assertTrue(postPort.releaseRequests().isEmpty());
        assertEquals(PostInfo.Status.COMPLETED, postPort.statusOf(POST_P));
        assertEquals(PostInfo.Status.COMPLETED, postPort.statusOf(POST_Q1));
    }

    @Test
    @DisplayName("NOT_MET 뒤 B가 이유를 남기면 해제 요청이 다시 가지 않고 P · Q1은 교환 가능 그대로다")
    void handle_reasonAfterNotMet_doesNotReleaseAgain() {
        // given
        handlerAt(NOW).handle(notMet(MEMBER_A));

        // when
        handlerAt(NOW.plus(Duration.ofHours(1))).handle(new RespondConfirmationCommand(APPOINTMENT_ID, MEMBER_B,
                ConfirmationAnswer.NOT_MET, NotMetReason.SELF_ABSENT, null));

        // then
        assertEquals(1, postPort.releaseRequests().size());
        assertTrue(postPort.completeRequests().isEmpty());
        assertEquals(PostInfo.Status.AVAILABLE, postPort.statusOf(POST_P));
        assertEquals(PostInfo.Status.AVAILABLE, postPort.statusOf(POST_Q1));
    }

    // ---- AC-16 ----

    @Test
    @DisplayName("DONE 처리 중 post BC가 완료를 거부하면 PostStatusRejectedException이 그대로 전파된다")
    void handle_completeRejected_propagates() {
        postPort.rejectStatusChangeOf(POST_Q1);

        PostStatusRejectedException exception = assertThrows(PostStatusRejectedException.class,
                () -> handlerAt(NOW).handle(done(MEMBER_A)));

        assertEquals(POST_Q1, exception.getPostId());
        assertEquals(1, postPort.completeRequests().size());
        assertEquals(PostInfo.Status.RESERVED, postPort.statusOf(POST_P));
        assertEquals(PostInfo.Status.RESERVED, postPort.statusOf(POST_Q1));
    }

    @Test
    @DisplayName("NOT_MET 처리 중 post BC가 해제를 거부하면 PostStatusRejectedException이 그대로 전파된다")
    void handle_releaseRejected_propagates() {
        postPort.rejectStatusChangeOf(POST_P);

        PostStatusRejectedException exception = assertThrows(PostStatusRejectedException.class,
                () -> handlerAt(NOW).handle(notMet(MEMBER_A)));

        assertEquals(POST_P, exception.getPostId());
        assertEquals(1, postPort.releaseRequests().size());
        assertEquals(PostInfo.Status.RESERVED, postPort.statusOf(POST_P));
        assertEquals(PostInfo.Status.RESERVED, postPort.statusOf(POST_Q1));
    }

    // ---- helpers ----

    private RespondConfirmationHandler handlerAt(Instant now) {
        ApplicationEventPublisher eventPublisher = publishedEvents::add;
        return new RespondConfirmationHandler(appointmentRepository, exchangeRepository, postLockRepository,
                postPort, new ServiceTime(Clock.fixed(now, ZoneId.of("Asia/Seoul"))), eventPublisher);
    }

    private static RespondConfirmationCommand done(Long memberId) {
        return new RespondConfirmationCommand(APPOINTMENT_ID, memberId, ConfirmationAnswer.DONE, null, null);
    }

    private static RespondConfirmationCommand notMet(Long memberId) {
        return new RespondConfirmationCommand(APPOINTMENT_ID, memberId, ConfirmationAnswer.NOT_MET,
                NotMetReason.NO_SHOW, null);
    }

    private static PostInfo post(Long id, Long ownerId, PostInfo.Status status) {
        return new PostInfo(id, "글 " + id, "post/" + id + "/1.jpg", ownerId, status);
    }
}
