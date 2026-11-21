package com.ject.exchange.application.service.command;

import com.ject.exchange.application.dto.command.CreateExchangeCommand;
import com.ject.exchange.application.dto.dto.NewItem;
import com.ject.exchange.application.dto.dto.PostInfo;
import com.ject.exchange.domain.domain.exchange.OfferType;
import com.ject.exchange.domain.exception.ExchangeErrorCode;
import com.ject.exchange.domain.exception.ExchangeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * E-1 교환 요청에서 커맨드의 요청자(로그인 회원) 기준 실패 규칙을 확인한다.
 */
class CreateExchangeHandlerLoginMemberTest {

    private static final Long POST_P = 10L;
    private static final Long POST_OF_C = 22L;
    private static final Long MEMBER_A = 101L;
    private static final Long MEMBER_B = 102L;
    private static final Long MEMBER_C = 103L;

    private FakeExchangeRepository exchangeRepository;
    private FakePostPort postPort;
    private CreateExchangeHandler handler;

    @BeforeEach
    void setUp() {
        exchangeRepository = new FakeExchangeRepository();
        postPort = new FakePostPort();
        handler = new CreateExchangeHandler(exchangeRepository, postPort);

        postPort.register(new PostInfo(POST_P, "치이카와 교환해요", null, MEMBER_B, PostInfo.Status.AVAILABLE));
        postPort.register(new PostInfo(POST_OF_C, "하치와레 키링", null, MEMBER_C, PostInfo.Status.AVAILABLE));
    }

    @Test
    @DisplayName("요청자가 글 게시자 본인이면 OWN_POST_REQUEST이고 저장하지 않는다")
    void handle_ownPost_throwsOwnPostRequest() {
        CreateExchangeCommand command = new CreateExchangeCommand(MEMBER_B, POST_P, OfferType.NEW, null,
                new NewItem("치이카와 아크릴 스탠드", false, "가챠에서 두 개 나와서 하나 교환해요"));

        ExchangeException exception = assertThrows(ExchangeException.class, () -> handler.handle(command));

        assertEquals(ExchangeErrorCode.OWN_POST_REQUEST, exception.getErrorCode());
        assertEquals(0, exchangeRepository.saveCount());
    }

    @Test
    @DisplayName("요청자가 남의 글을 POST 방식으로 첨부하면 POST_NOT_ATTACHABLE이고 저장하지 않는다")
    void handle_attachOthersPost_throwsPostNotAttachable() {
        CreateExchangeCommand command = new CreateExchangeCommand(MEMBER_A, POST_P, OfferType.POST,
                List.of(POST_OF_C), null);

        ExchangeException exception = assertThrows(ExchangeException.class, () -> handler.handle(command));

        assertEquals(ExchangeErrorCode.POST_NOT_ATTACHABLE, exception.getErrorCode());
        assertEquals(0, exchangeRepository.saveCount());
    }
}
