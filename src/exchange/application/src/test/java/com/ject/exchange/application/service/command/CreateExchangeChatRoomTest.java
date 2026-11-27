package com.ject.exchange.application.service.command;

import com.ject.exchange.application.dto.command.CreateExchangeCommand;
import com.ject.exchange.application.dto.dto.NewItem;
import com.ject.exchange.application.dto.dto.PostInfo;
import com.ject.exchange.domain.domain.exchange.Exchange;
import com.ject.exchange.domain.domain.exchange.OfferType;
import com.ject.exchange.domain.exception.ExchangeErrorCode;
import com.ject.exchange.domain.exception.ExchangeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
class CreateExchangeChatRoomTest {

    private static final Long POST_ID = 10L;
    private static final Long REQUESTER_ID = 100L;
    private static final Long OWNER_ID = 200L;
    private static final Long EXISTING_EXCHANGE_ID = 30L;
    private static final Long EXISTING_CHAT_ROOM_ID = 40L;

    private FakeExchangeRepository exchangeRepository;
    private FakeImagePort imagePort;
    private FakeChatRoomPort chatRoomPort;
    private CreateExchangeHandler handler;

    @BeforeEach
    void setUp() {
        exchangeRepository = new FakeExchangeRepository();
        FakePostPort postPort = new FakePostPort();
        postPort.register(new PostInfo(POST_ID, "치이카와 교환해요", null, OWNER_ID, PostInfo.Status.AVAILABLE));
        imagePort = new FakeImagePort();
        chatRoomPort = new FakeChatRoomPort();
        handler = new CreateExchangeHandler(exchangeRepository, postPort, new FakeUserPort(), imagePort, chatRoomPort);
    }

    @Test
    @DisplayName("신규 교환 요청을 저장한 뒤 같은 교환 ID로 채팅방을 생성한다")
    void handle_newRequest_createsChatRoom() {
        Long exchangeId = handler.handle(command());

        assertEquals(1, exchangeRepository.storedCount());
        assertEquals(1, chatRoomPort.createCount());
        assertEquals(7_000L, chatRoomPort.findChatRoomIdByExchangeId(exchangeId).orElseThrow());
    }

    @Test
    @DisplayName("현재 방이 이미 있으면 새 교환과 사진과 채팅방을 만들지 않고 기존 교환을 반환한다")
    void handle_activeRoom_returnsExistingExchangeWithoutWrites() {
        exchangeRepository.preload(existingExchange());
        chatRoomPort.active(EXISTING_EXCHANGE_ID, EXISTING_CHAT_ROOM_ID);

        Long exchangeId = handler.handle(command());

        assertEquals(EXISTING_EXCHANGE_ID, exchangeId);
        assertEquals(1, exchangeRepository.storedCount());
        assertEquals(0, exchangeRepository.saveCount());
        assertEquals(0, chatRoomPort.createCount());
        assertEquals(0, imagePort.calls().size());
    }

    @Test
    @DisplayName("나간 방이 있으면 EXCHANGE_011이고 새 데이터를 만들지 않는다")
    void handle_leftRoom_throwsExchange011() {
        chatRoomPort.left(EXISTING_EXCHANGE_ID);

        ExchangeException exception = assertThrows(ExchangeException.class, () -> handler.handle(command()));

        assertEquals(ExchangeErrorCode.LEFT_CHAT_ROOM_REQUEST, exception.getErrorCode());
        assertEquals(0, exchangeRepository.storedCount());
        assertEquals(0, chatRoomPort.createCount());
        assertEquals(0, imagePort.calls().size());
    }

    @Test
    @DisplayName("채팅방 생성 실패를 삼키지 않아 외부 트랜잭션이 롤백할 수 있게 한다")
    void handle_chatRoomCreateFailure_propagates() {
        IllegalStateException failure = new IllegalStateException("채팅방 저장 실패");
        chatRoomPort.failCreate(failure);

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> handler.handle(command()));

        assertEquals(failure, thrown);
        assertEquals(List.of(List.of("exchange/1000/item.jpg")), imagePort.discardCalls());
    }

    @Test
    @DisplayName("보상 삭제도 실패하면 채팅방 생성 예외를 유지하고 삭제 예외를 suppressed로 남긴다")
    void handle_compensationFailure_preservesOriginalFailure() {
        IllegalStateException createFailure = new IllegalStateException("채팅방 저장 실패");
        IllegalArgumentException discardFailure = new IllegalArgumentException("이미지 삭제 실패");
        chatRoomPort.failCreate(createFailure);
        imagePort.failDiscard(discardFailure);

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> handler.handle(command()));

        assertSame(createFailure, thrown);
        assertEquals(1, thrown.getSuppressed().length);
        assertSame(discardFailure, thrown.getSuppressed()[0]);
        assertEquals(List.of(List.of("exchange/1000/item.jpg")), imagePort.discardCalls());
    }

    private static CreateExchangeCommand command() {
        return new CreateExchangeCommand(REQUESTER_ID, POST_ID, OfferType.NEW, null,
                new NewItem("우사기 아크릴 스탠드", false, null, List.of("exchange/tmp/item.jpg")));
    }

    private static Exchange existingExchange() {
        return Exchange.of(EXISTING_EXCHANGE_ID, POST_ID, REQUESTER_ID, OWNER_ID, OfferType.NEW,
                List.of(), com.ject.exchange.domain.domain.exchange.ExchangeStatus.ACTIVE,
                null, null, null);
    }
}
