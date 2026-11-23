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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * E-1 교환 요청이 post BC의 글 정보 · 글 상태로 판정하는지 확인한다 (#107).
 */
class CreateExchangePostConnectTest {

    /** B의 글 P */
    private static final Long POST_P = 10L;
    /** A의 글 Q1 */
    private static final Long POST_Q1 = 20L;
    /** A의 글 Q2 */
    private static final Long POST_Q2 = 21L;
    /** B의 다른 글 */
    private static final Long POST_OF_B = 30L;
    private static final Long MISSING_POST = 999L;
    /** 예전 stub 글 id. post BC에 없으면 다른 없는 글과 같다 */
    private static final Long LEGACY_STUB_POST = 1L;
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

        postPort.register(post(POST_P, "P", "post/10/1.jpg", MEMBER_B, PostInfo.Status.AVAILABLE));
        postPort.register(post(POST_Q1, "Q1", "post/20/1.jpg", MEMBER_A, PostInfo.Status.AVAILABLE));
        postPort.register(post(POST_Q2, "Q2", null, MEMBER_A, PostInfo.Status.AVAILABLE));
        postPort.register(post(POST_OF_B, "B의 다른 글", null, MEMBER_B, PostInfo.Status.AVAILABLE));
    }

    // ---- AC-1 ----

    @Test
    @DisplayName("POST 방식으로 P에 요청하면 저장된 교환의 게시자는 P의 작성자 B이고 첨부 글은 Q1이다")
    void handle_postOffer_ownerIsPostAuthor() {
        // when
        Long exchangeId = handler.handle(postCommand(MEMBER_A, POST_P, List.of(POST_Q1)));

        // then
        Exchange saved = exchangeRepository.findById(exchangeId).orElseThrow();
        assertEquals(MEMBER_A, saved.getRequestUserId());
        assertEquals(MEMBER_B, saved.getOwnerId());
        assertEquals(POST_P, saved.getPostId());
        assertEquals(POST_Q1, saved.getOfferedItems().get(0).getSourcePostId());
        assertEquals(1, exchangeRepository.saveCount());
    }

    @Test
    @DisplayName("예전 stub 글 1번이 post BC에 없으면 POST_NOT_FOUND이고 저장하지 않는다")
    void handle_legacyStubPostMissing_throwsPostNotFound() {
        assertRejected(ExchangeErrorCode.POST_NOT_FOUND,
                () -> handler.handle(newItemCommand(MEMBER_A, LEGACY_STUB_POST)));
    }

    // ---- AC-2 · AC-3 ----

    @Test
    @DisplayName("post BC에 없는 글이면 POST_NOT_FOUND이고 저장하지 않는다")
    void handle_missingPost_throwsPostNotFound() {
        assertRejected(ExchangeErrorCode.POST_NOT_FOUND,
                () -> handler.handle(newItemCommand(MEMBER_A, MISSING_POST)));
    }

    @Test
    @DisplayName("삭제된 글이면 NEW 방식 요청은 POST_NOT_FOUND이고 저장하지 않는다")
    void handle_deletedPost_newItem_throwsPostNotFound() {
        postPort.register(post(POST_P, "P", "post/10/1.jpg", MEMBER_B, PostInfo.Status.DELETED));

        assertRejected(ExchangeErrorCode.POST_NOT_FOUND,
                () -> handler.handle(newItemCommand(MEMBER_A, POST_P)));
    }

    @Test
    @DisplayName("삭제된 글이면 POST 방식 요청도 POST_NOT_FOUND이고 저장하지 않는다")
    void handle_deletedPost_postOffer_throwsPostNotFound() {
        postPort.register(post(POST_P, "P", "post/10/1.jpg", MEMBER_B, PostInfo.Status.DELETED));

        assertRejected(ExchangeErrorCode.POST_NOT_FOUND,
                () -> handler.handle(postCommand(MEMBER_A, POST_P, List.of(POST_Q1))));
    }

    @Test
    @DisplayName("삭제된 본인 글에 요청해도 OWN_POST_REQUEST보다 POST_NOT_FOUND가 먼저다")
    void handle_deletedOwnPost_postNotFoundFirst() {
        postPort.register(post(POST_P, "P", null, MEMBER_B, PostInfo.Status.DELETED));

        assertRejected(ExchangeErrorCode.POST_NOT_FOUND,
                () -> handler.handle(newItemCommand(MEMBER_B, POST_P)));
    }

    // ---- AC-4 ----

    @Test
    @DisplayName("예약중 글이면 POST_RESERVED_ELSEWHERE이고 저장하지 않는다")
    void handle_reservedPost_throwsPostReservedElsewhere() {
        postPort.register(post(POST_P, "P", null, MEMBER_B, PostInfo.Status.RESERVED));

        assertRejected(ExchangeErrorCode.POST_RESERVED_ELSEWHERE,
                () -> handler.handle(postCommand(MEMBER_A, POST_P, List.of(POST_Q1))));
    }

    @Test
    @DisplayName("교환 완료 글이면 POST_NOT_REQUESTABLE이고 저장하지 않는다")
    void handle_completedPost_throwsPostNotRequestable() {
        postPort.register(post(POST_P, "P", null, MEMBER_B, PostInfo.Status.COMPLETED));

        assertRejected(ExchangeErrorCode.POST_NOT_REQUESTABLE,
                () -> handler.handle(postCommand(MEMBER_A, POST_P, List.of(POST_Q1))));
    }

    @Test
    @DisplayName("예약중인 본인 글에 요청하면 OWN_POST_REQUEST보다 POST_RESERVED_ELSEWHERE가 먼저다")
    void handle_reservedOwnPost_reservedFirst() {
        postPort.register(post(POST_P, "P", null, MEMBER_B, PostInfo.Status.RESERVED));

        assertRejected(ExchangeErrorCode.POST_RESERVED_ELSEWHERE,
                () -> handler.handle(newItemCommand(MEMBER_B, POST_P)));
    }

    @Test
    @DisplayName("교환 완료 글이고 차단 관계여도 POST_NOT_REQUESTABLE이 먼저다")
    void handle_completedPostWithBlock_notRequestableFirst() {
        postPort.register(post(POST_P, "P", null, MEMBER_B, PostInfo.Status.COMPLETED));
        userPort.block(MEMBER_A, MEMBER_B);

        assertRejected(ExchangeErrorCode.POST_NOT_REQUESTABLE,
                () -> handler.handle(newItemCommand(MEMBER_A, POST_P)));
    }

    // ---- AC-5 ----

    @Test
    @DisplayName("첨부 글이 게시자 B의 글이면 POST_NOT_ATTACHABLE이고 저장하지 않는다")
    void handle_attachOwnersPost_throwsPostNotAttachable() {
        assertRejected(ExchangeErrorCode.POST_NOT_ATTACHABLE,
                () -> handler.handle(postCommand(MEMBER_A, POST_P, List.of(POST_OF_B))));
    }

    @ParameterizedTest
    @EnumSource(value = PostInfo.Status.class, names = {"DELETED", "RESERVED", "COMPLETED"})
    @DisplayName("첨부 글 Q1이 삭제 · 예약중 · 교환 완료면 POST_NOT_ATTACHABLE이고 저장하지 않는다")
    void handle_attachUnavailableOwnPost_throwsPostNotAttachable(PostInfo.Status status) {
        postPort.register(post(POST_Q1, "Q1", "post/20/1.jpg", MEMBER_A, status));

        assertRejected(ExchangeErrorCode.POST_NOT_ATTACHABLE,
                () -> handler.handle(postCommand(MEMBER_A, POST_P, List.of(POST_Q1))));
    }

    @Test
    @DisplayName("첨부 글이 post BC에 없으면 POST_NOT_ATTACHABLE이고 저장하지 않는다")
    void handle_attachMissingPost_throwsPostNotAttachable() {
        assertRejected(ExchangeErrorCode.POST_NOT_ATTACHABLE,
                () -> handler.handle(postCommand(MEMBER_A, POST_P, List.of(POST_Q1, MISSING_POST))));
    }

    @Test
    @DisplayName("첨부 글 Q1 · Q2 중 Q2만 예약중이어도 POST_NOT_ATTACHABLE이다")
    void handle_oneOfAttachedReserved_throwsPostNotAttachable() {
        postPort.register(post(POST_Q2, "Q2", null, MEMBER_A, PostInfo.Status.RESERVED));

        assertRejected(ExchangeErrorCode.POST_NOT_ATTACHABLE,
                () -> handler.handle(postCommand(MEMBER_A, POST_P, List.of(POST_Q1, POST_Q2))));
    }

    @Test
    @DisplayName("첨부 글 Q1 · Q2가 모두 교환 가능하면 저장된다")
    void handle_attachTwoAvailable_saves() {
        Long exchangeId = handler.handle(postCommand(MEMBER_A, POST_P, List.of(POST_Q1, POST_Q2)));

        Exchange saved = exchangeRepository.findById(exchangeId).orElseThrow();
        assertEquals(2, saved.getOfferedItems().size());
    }

    // ---- AC-11 ----

    @Test
    @DisplayName("P가 다른 교환의 약속 확정으로 예약중이면 C의 새 요청은 POST_RESERVED_ELSEWHERE다")
    void handle_byOtherMemberAfterReserved_throwsPostReservedElsewhere() {
        postPort.register(post(POST_P, "P", null, MEMBER_B, PostInfo.Status.RESERVED));

        assertRejected(ExchangeErrorCode.POST_RESERVED_ELSEWHERE,
                () -> handler.handle(newItemCommand(MEMBER_C, POST_P)));
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

    private static CreateExchangeCommand postCommand(Long requesterId, Long postId, List<Long> offeredPostIds) {
        return new CreateExchangeCommand(requesterId, postId, OfferType.POST, offeredPostIds, null);
    }

    private static PostInfo post(Long id, String title, String thumbnailKey, Long ownerId, PostInfo.Status status) {
        return new PostInfo(id, title, thumbnailKey, ownerId, status);
    }
}
