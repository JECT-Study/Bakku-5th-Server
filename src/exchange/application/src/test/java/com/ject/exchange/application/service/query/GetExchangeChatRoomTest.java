package com.ject.exchange.application.service.query;

import com.ject.common.core.time.ServiceTime;
import com.ject.exchange.application.dto.dto.ExchangeResult;
import com.ject.exchange.application.dto.dto.PostInfo;
import com.ject.exchange.application.dto.dto.UserInfo;
import com.ject.exchange.application.dto.query.GetExchangeQuery;
import com.ject.exchange.domain.domain.exchange.Exchange;
import com.ject.exchange.domain.domain.exchange.ExchangeStatus;
import com.ject.exchange.domain.domain.exchange.OfferType;
import com.ject.exchange.domain.repository.AppointmentRepository;
import com.ject.exchange.domain.repository.ExchangeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
class GetExchangeChatRoomTest {

    private static final Instant NOW = Instant.parse("2026-11-27T03:00:00Z");

    @Test
    @DisplayName("교환 상세는 chat BC에서 조회한 실제 채팅방 ID를 반환한다")
    void handle_returnsLinkedChatRoomId() {
        Long exchangeId = 1L;
        Long postId = 10L;
        Long requesterId = 100L;
        Long ownerId = 200L;
        Long chatRoomId = 500L;
        FakeExchangeRepository exchangeRepository = new FakeExchangeRepository();
        exchangeRepository.preload(Exchange.of(exchangeId, postId, requesterId, ownerId, OfferType.NEW,
                List.of(), ExchangeStatus.ACTIVE, null, NOW, NOW));
        FakeAppointmentRepository appointmentRepository = new FakeAppointmentRepository(exchangeRepository, NOW);
        FakePostPort postPort = new FakePostPort();
        postPort.register(new PostInfo(postId, "치이카와 교환해요", null, ownerId, PostInfo.Status.AVAILABLE));
        FakeUserPort userPort = new FakeUserPort();
        userPort.register(new UserInfo(requesterId, "요청자", 0));
        userPort.register(new UserInfo(ownerId, "게시자", 0));
        FakeChatRoomPort chatRoomPort = new FakeChatRoomPort();
        chatRoomPort.register(exchangeId, chatRoomId);
        GetExchangeHandler handler = new GetExchangeHandler(exchangeRepository, appointmentRepository, postPort,
                userPort, new ServiceTime(Clock.fixed(NOW, ZoneOffset.UTC)), chatRoomPort);

        ExchangeResult result = handler.handle(GetExchangeQuery.builder()
                .exchangeId(exchangeId).viewerId(requesterId).build());

        assertEquals(chatRoomId, result.chatRoomId());
    }
}
