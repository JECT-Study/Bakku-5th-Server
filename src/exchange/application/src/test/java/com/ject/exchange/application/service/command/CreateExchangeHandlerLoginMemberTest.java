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
import org.junit.jupiter.api.function.Executable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * E-1 교환 요청에서 커맨드의 요청자(로그인 회원) 기준 실패 규칙과 차단 관계 판정을 확인한다.
 */
class CreateExchangeHandlerLoginMemberTest {

    private static final Long POST_P = 10L;
    private static final Long POST_Q_OF_A = 21L;
    private static final Long POST_OF_C = 22L;
    private static final Long MISSING_POST = 999L;
    private static final Long MEMBER_A = 101L;
    private static final Long MEMBER_B = 102L;
    private static final Long MEMBER_C = 103L;

    private FakeExchangeRepository exchangeRepository;
    private FakePostPort postPort;
    private FakeUserPort userPort;
    private CreateExchangeHandler handler;

    @BeforeEach
    void setUp() {
        exchangeRepository = new FakeExchangeRepository();
        postPort = new FakePostPort();
        userPort = new FakeUserPort();
        handler = new CreateExchangeHandler(exchangeRepository, postPort, userPort);

        postPort.register(new PostInfo(POST_P, "치이카와 교환해요", null, MEMBER_B, PostInfo.Status.AVAILABLE));
        postPort.register(new PostInfo(POST_Q_OF_A, "우사기 미니 피규어", null, MEMBER_A, PostInfo.Status.AVAILABLE));
        postPort.register(new PostInfo(POST_OF_C, "하치와레 키링", null, MEMBER_C, PostInfo.Status.AVAILABLE));
    }

    // ---- AC-1 · AC-8 (#74) ----

    @Test
    @DisplayName("차단 관계가 없으면 커맨드의 요청자로 교환이 저장된다")
    void handle_noBlock_savesWithRequester() {
        Long exchangeId = handler.handle(newItemCommand(MEMBER_A, POST_P));

        assertNotNull(exchangeId);
        Exchange saved = exchangeRepository.findById(exchangeId).orElseThrow();
        assertEquals(MEMBER_A, saved.getRequestUserId());
        assertEquals(MEMBER_B, saved.getOwnerId());
        assertEquals(1, exchangeRepository.saveCount());
    }

    @Test
    @DisplayName("요청자가 글 게시자 본인이면 OWN_POST_REQUEST이고 저장하지 않는다")
    void handle_ownPost_throwsOwnPostRequest() {
        assertRejected(ExchangeErrorCode.OWN_POST_REQUEST, () -> handler.handle(newItemCommand(MEMBER_B, POST_P)));
    }

    @Test
    @DisplayName("요청자가 남의 글을 POST 방식으로 첨부하면 POST_NOT_ATTACHABLE이고 저장하지 않는다")
    void handle_attachOthersPost_throwsPostNotAttachable() {
        assertRejected(ExchangeErrorCode.POST_NOT_ATTACHABLE,
                () -> handler.handle(postCommand(MEMBER_A, POST_P, POST_OF_C)));
    }

    // ---- AC-9 차단 관계 ----

    @Test
    @DisplayName("요청자가 게시자를 차단했으면 NEW 방식 요청은 BLOCKED_USER이고 저장하지 않는다")
    void handle_requesterBlockedOwner_newItem_throwsBlockedUser() {
        userPort.block(MEMBER_A, MEMBER_B);

        assertRejected(ExchangeErrorCode.BLOCKED_USER, () -> handler.handle(newItemCommand(MEMBER_A, POST_P)));
    }

    @Test
    @DisplayName("게시자가 요청자를 차단했으면 NEW 방식 요청은 BLOCKED_USER이고 저장하지 않는다")
    void handle_ownerBlockedRequester_newItem_throwsBlockedUser() {
        userPort.block(MEMBER_B, MEMBER_A);

        assertRejected(ExchangeErrorCode.BLOCKED_USER, () -> handler.handle(newItemCommand(MEMBER_A, POST_P)));
    }

    @Test
    @DisplayName("요청자가 게시자를 차단했으면 POST 방식 요청도 BLOCKED_USER이고 저장하지 않는다")
    void handle_requesterBlockedOwner_post_throwsBlockedUser() {
        userPort.block(MEMBER_A, MEMBER_B);

        assertRejected(ExchangeErrorCode.BLOCKED_USER,
                () -> handler.handle(postCommand(MEMBER_A, POST_P, POST_Q_OF_A)));
    }

    @Test
    @DisplayName("게시자가 요청자를 차단했으면 POST 방식 요청도 BLOCKED_USER이고 저장하지 않는다")
    void handle_ownerBlockedRequester_post_throwsBlockedUser() {
        userPort.block(MEMBER_B, MEMBER_A);

        assertRejected(ExchangeErrorCode.BLOCKED_USER,
                () -> handler.handle(postCommand(MEMBER_A, POST_P, POST_Q_OF_A)));
    }

    @Test
    @DisplayName("차단을 해제하면 같은 요청이 저장된다")
    void handle_afterUnblock_saves() {
        userPort.block(MEMBER_A, MEMBER_B);
        assertRejected(ExchangeErrorCode.BLOCKED_USER, () -> handler.handle(newItemCommand(MEMBER_A, POST_P)));

        userPort.unblock(MEMBER_A, MEMBER_B);
        Long exchangeId = handler.handle(newItemCommand(MEMBER_A, POST_P));

        assertEquals(MEMBER_A, exchangeRepository.findById(exchangeId).orElseThrow().getRequestUserId());
        assertEquals(1, exchangeRepository.saveCount());
    }

    @Test
    @DisplayName("차단 판정은 요청자와 글 게시자로 묻는다")
    void handle_checksBlockBetweenRequesterAndOwner() {
        handler.handle(newItemCommand(MEMBER_A, POST_P));

        assertEquals(List.of(List.of(MEMBER_A, MEMBER_B)), userPort.blockChecks());
    }

    @Test
    @DisplayName("요청자가 제3자를 차단했어도 게시자와 차단 관계가 아니면 저장된다")
    void handle_blockWithThirdParty_saves() {
        userPort.block(MEMBER_A, MEMBER_C);

        Long exchangeId = handler.handle(newItemCommand(MEMBER_A, POST_P));

        assertNotNull(exchangeId);
        assertEquals(1, exchangeRepository.saveCount());
    }

    // ---- AC-10 판정 순서 009 → 005 → 003 → 008 → 007 ----

    @Test
    @DisplayName("없는 글이면 차단 관계여도 POST_NOT_FOUND가 먼저다")
    void handle_missingPostWithBlock_postNotFoundFirst() {
        userPort.block(MEMBER_A, MEMBER_B);

        assertRejected(ExchangeErrorCode.POST_NOT_FOUND,
                () -> handler.handle(newItemCommand(MEMBER_A, MISSING_POST)));
    }

    @Test
    @DisplayName("글이 예약중이면 차단 관계여도 POST_NOT_REQUESTABLE이 먼저다")
    void handle_reservedPostWithBlock_notRequestableFirst() {
        postPort.register(new PostInfo(POST_P, "치이카와 교환해요", null, MEMBER_B, PostInfo.Status.RESERVED));
        userPort.block(MEMBER_A, MEMBER_B);

        assertRejected(ExchangeErrorCode.POST_NOT_REQUESTABLE,
                () -> handler.handle(newItemCommand(MEMBER_A, POST_P)));
    }

    @Test
    @DisplayName("본인 글 요청이면 차단 판정 전에 OWN_POST_REQUEST다")
    void handle_ownPostWithBlockRegistered_ownPostFirst() {
        // 본인과의 차단 관계는 실제로 생기지 않지만 판정 순서를 드러내려고 등록한다
        userPort.block(MEMBER_B, MEMBER_B);

        assertRejected(ExchangeErrorCode.OWN_POST_REQUEST, () -> handler.handle(newItemCommand(MEMBER_B, POST_P)));
    }

    @Test
    @DisplayName("차단 관계이면서 남의 글을 첨부하면 BLOCKED_USER가 POST_NOT_ATTACHABLE보다 먼저다")
    void handle_blockWithOthersAttachedPost_blockedFirst() {
        userPort.block(MEMBER_B, MEMBER_A);

        assertRejected(ExchangeErrorCode.BLOCKED_USER,
                () -> handler.handle(postCommand(MEMBER_A, POST_P, POST_OF_C)));
    }

    // ---- helpers ----

    private void assertRejected(ExchangeErrorCode expected, Executable action) {
        int savesBefore = exchangeRepository.saveCount();

        ExchangeException exception = assertThrows(ExchangeException.class, action);

        assertEquals(expected, exception.getErrorCode());
        assertEquals(savesBefore, exchangeRepository.saveCount());
    }

    private static CreateExchangeCommand newItemCommand(Long requesterId, Long postId) {
        return new CreateExchangeCommand(requesterId, postId, OfferType.NEW, null,
                new NewItem("치이카와 아크릴 스탠드", false, "가챠에서 두 개 나와서 하나 교환해요"));
    }

    private static CreateExchangeCommand postCommand(Long requesterId, Long postId, Long offeredPostId) {
        return new CreateExchangeCommand(requesterId, postId, OfferType.POST, List.of(offeredPostId), null);
    }
}
