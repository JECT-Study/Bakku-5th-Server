package com.ject.exchange.application.service.command;

import com.ject.common.core.time.ServiceTime;
import com.ject.exchange.application.dto.command.ProposeAppointmentCommand;
import com.ject.exchange.application.dto.dto.PostInfo;
import com.ject.exchange.domain.domain.appointment.AppointmentTimeType;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A-1 약속 제안이 post BC 글 상태로 막히는지 확인한다 (#107).
 */
class ProposeAppointmentPostConnectTest {

    /** 2026-10-26 14:00 (+09:00) */
    private static final Instant NOW = Instant.parse("2026-10-26T05:00:00Z");
    /** 2026-10-27 15:00 (+09:00) */
    private static final Instant SCHEDULED_AT = Instant.parse("2026-10-27T06:00:00Z");
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
    private FakePostPort postPort;
    private ProposeAppointmentHandler handler;

    @BeforeEach
    void setUp() {
        exchangeRepository = new FakeExchangeRepository();
        appointmentRepository = new FakeAppointmentRepository(exchangeRepository, NOW);
        postPort = new FakePostPort();
        handler = new ProposeAppointmentHandler(exchangeRepository, appointmentRepository, postPort,
                new FakeUserPort(), new ServiceTime(Clock.fixed(NOW, ZoneId.of("Asia/Seoul"))));

        postPort.register(post(POST_P, MEMBER_B, PostInfo.Status.AVAILABLE));
        postPort.register(post(POST_Q1, MEMBER_A, PostInfo.Status.AVAILABLE));
        postPort.register(post(POST_Q2, MEMBER_A, PostInfo.Status.AVAILABLE));
        exchangeRepository.preload(Exchange.of(EXCHANGE_X, POST_P, MEMBER_A, MEMBER_B, OfferType.POST,
                List.of(OfferedItem.of(ITEM_Q1, POST_Q1, null, null, null, 0),
                        OfferedItem.of(ITEM_Q2, POST_Q2, null, null, null, 1)),
                ExchangeStatus.ACTIVE, null, NOW, NOW));
    }

    @Test
    @DisplayName("P · Q1이 교환 가능하면 Q1을 고른 제안이 저장된다")
    void handle_allAvailable_saves() {
        Long appointmentId = handler.handle(command(List.of(ITEM_Q1)));

        assertNotNull(appointmentId);
        assertEquals(1, appointmentRepository.saveCount());
    }

    @Test
    @DisplayName("P가 예약중이면 제안은 POST_RESERVED_ELSEWHERE이고 저장하지 않는다")
    void handle_postReserved_throwsPostReservedElsewhere() {
        postPort.register(post(POST_P, MEMBER_B, PostInfo.Status.RESERVED));

        assertRejected(ExchangeErrorCode.POST_RESERVED_ELSEWHERE);
    }

    @ParameterizedTest
    @EnumSource(value = PostInfo.Status.class, names = {"COMPLETED", "DELETED"})
    @DisplayName("P가 교환 완료 · 삭제면 제안은 POST_NOT_REQUESTABLE이고 저장하지 않는다")
    void handle_postCompletedOrDeleted_throwsPostNotRequestable(PostInfo.Status status) {
        postPort.register(post(POST_P, MEMBER_B, status));

        assertRejected(ExchangeErrorCode.POST_NOT_REQUESTABLE);
    }

    @Test
    @DisplayName("P가 post BC에서 조회되지 않으면 제안은 POST_NOT_REQUESTABLE이다")
    void handle_postMissing_throwsPostNotRequestable() {
        exchangeRepository.preload(Exchange.of(EXCHANGE_X, 999L, MEMBER_A, MEMBER_B, OfferType.POST,
                List.of(OfferedItem.of(ITEM_Q1, POST_Q1, null, null, null, 0)),
                ExchangeStatus.ACTIVE, null, NOW, NOW));

        assertRejected(ExchangeErrorCode.POST_NOT_REQUESTABLE);
    }

    private void assertRejected(ExchangeErrorCode expected) {
        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> handler.handle(command(List.of(ITEM_Q1))));

        assertEquals(expected, exception.getErrorCode());
        assertEquals(0, appointmentRepository.saveCount());
    }

    private static ProposeAppointmentCommand command(List<Long> itemIds) {
        return new ProposeAppointmentCommand(EXCHANGE_X, MEMBER_A, AppointmentTimeType.SCHEDULED, SCHEDULED_AT,
                PLACE, itemIds, 30);
    }

    private static PostInfo post(Long id, Long ownerId, PostInfo.Status status) {
        return new PostInfo(id, "글 " + id, "post/" + id + "/1.jpg", ownerId, status);
    }
}
