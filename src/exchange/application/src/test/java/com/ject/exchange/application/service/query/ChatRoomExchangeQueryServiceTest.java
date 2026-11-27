package com.ject.exchange.application.service.query;

import com.ject.common.core.time.ServiceTime;
import com.ject.exchange.application.contract.ChatRoomAppointmentState;
import com.ject.exchange.application.contract.ChatRoomExchangeState;
import com.ject.exchange.application.service.contract.ChatRoomExchangeQueryService;
import com.ject.exchange.domain.domain.appointment.Appointment;
import com.ject.exchange.domain.domain.appointment.AppointmentStatus;
import com.ject.exchange.domain.domain.appointment.AppointmentTimeType;
import com.ject.exchange.domain.domain.appointment.vo.AppointmentItemIds;
import com.ject.exchange.domain.domain.appointment.vo.AppointmentTime;
import com.ject.exchange.domain.domain.appointment.vo.Place;
import com.ject.exchange.domain.domain.exchange.Exchange;
import com.ject.exchange.domain.domain.exchange.ExchangeStatus;
import com.ject.exchange.domain.domain.exchange.OfferType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
class ChatRoomExchangeQueryServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-28T06:00:00Z");
    private static final Long EXCHANGE_ID = 1L;
    private static final Long OTHER_EXCHANGE_ID = 2L;
    private static final Long POST_ID = 10L;
    private static final Long REQUESTER_ID = 101L;
    private static final Long OWNER_ID = 202L;
    private static final Place PLACE = new Place("홍대입구역", "서울 마포구", 37.55, 126.92);

    private FakeExchangeRepository exchangeRepository;
    private FakeAppointmentRepository appointmentRepository;
    private ChatRoomExchangeQueryService service;

    @BeforeEach
    void setUp() {
        exchangeRepository = new FakeExchangeRepository();
        appointmentRepository = new FakeAppointmentRepository(exchangeRepository, NOW);
        service = new ChatRoomExchangeQueryService(exchangeRepository, appointmentRepository,
                new ServiceTime(Clock.fixed(NOW, ZoneId.of("Asia/Seoul"))));
        exchangeRepository.preload(exchange(EXCHANGE_ID, REQUESTER_ID, ExchangeStatus.ACTIVE));
    }

    @Test
    @DisplayName("같은 게시글의 다른 교환이 완료됐으면 다른 교환 완료 상태다")
    void getChatRoomState_otherExchangeCompleted_returnsTrue() {
        exchangeRepository.preload(exchange(OTHER_EXCHANGE_ID, 303L, ExchangeStatus.COMPLETED));

        ChatRoomExchangeState state = service.getChatRoomState(EXCHANGE_ID, REQUESTER_ID);

        assertTrue(state.otherExchangeCompleted());
    }

    @Test
    @DisplayName("교환 확인 개시 시각과 같으면 확인 질문이 열린 상태다")
    void getChatRoomState_atConfirmationBoundary_returnsOpened() {
        appointmentRepository.preload(confirmedAppointment(NOW.minusSeconds(30 * 60)));

        ChatRoomExchangeState state = service.getChatRoomState(EXCHANGE_ID, REQUESTER_ID);

        assertEquals(ChatRoomAppointmentState.CONFIRMATION_OPENED, state.appointmentState());
    }

    private static Exchange exchange(Long id, Long requesterId, ExchangeStatus status) {
        return Exchange.of(id, POST_ID, requesterId, OWNER_ID, OfferType.POST, List.of(), status,
                status == ExchangeStatus.COMPLETED ? NOW : null, NOW, NOW);
    }

    private static Appointment confirmedAppointment(Instant scheduledAt) {
        return Appointment.of(3L, EXCHANGE_ID, REQUESTER_ID, OWNER_ID, AppointmentStatus.CONFIRMED,
                new AppointmentTime(AppointmentTimeType.SCHEDULED, scheduledAt), PLACE,
                new AppointmentItemIds(List.of(31L)), null, 30, 30, NOW.minusSeconds(60 * 60), NOW, NOW);
    }
}
