package com.ject.exchange.application.service.command;

import com.ject.exchange.application.dto.command.CreateExchangeCommand;
import com.ject.exchange.application.dto.dto.NewItem;
import com.ject.exchange.application.dto.dto.PostInfo;
import com.ject.exchange.domain.domain.exchange.Exchange;
import com.ject.exchange.domain.domain.exchange.OfferType;
import com.ject.exchange.domain.domain.exchange.OfferedItem;
import com.ject.exchange.domain.exception.ExchangeErrorCode;
import com.ject.exchange.domain.exception.ExchangeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E-1 NEW 방식 요청이 사진 tmp key를 이 교환의 정식 key로 확정해 저장하는지 확인한다 (#108).
 */
class CreateExchangeImageTest {

    private static final Long MEMBER_A = 101L;
    private static final Long MEMBER_B = 102L;
    /** B의 글 P */
    private static final Long POST_P = 10L;
    /** A의 글 Q1 */
    private static final Long POST_Q1 = 20L;
    private static final Long MISSING_POST = 999L;
    private static final String K1 = "exchange/tmp/101/0b1c2d3e-0001.jpg";
    private static final String K2 = "exchange/tmp/101/0b1c2d3e-0002.jpg";
    private static final String K3 = "exchange/tmp/101/0b1c2d3e-0003.jpg";
    private static final String OTHER_MEMBER_TMP_KEY = "exchange/tmp/103/0b1c2d3e-0009.jpg";

    private FakeExchangeRepository exchangeRepository;
    private FakePostPort postPort;
    private FakeUserPort userPort;
    private FakeImagePort imagePort;
    private CreateExchangeHandler handler;

    @BeforeEach
    void setUp() {
        exchangeRepository = new FakeExchangeRepository();
        postPort = new FakePostPort();
        userPort = new FakeUserPort();
        imagePort = new FakeImagePort();
        handler = new CreateExchangeHandler(exchangeRepository, postPort, userPort, imagePort);

        postPort.register(new PostInfo(POST_P, "P", "post/10/1.jpg", MEMBER_B, PostInfo.Status.AVAILABLE));
        postPort.register(new PostInfo(POST_Q1, "Q1", "post/20/1.jpg", MEMBER_A, PostInfo.Status.AVAILABLE));
    }

    // ---- AC-1 ----

    @Test
    @DisplayName("NEW 방식으로 사진 [K1, K2]를 보내면 저장된 아이템 사진은 이 교환의 정식 key [K1, K2] 순서다")
    void handle_newItem_savesFinalizedImagesInOrder() {
        // when
        Long exchangeId = handler.handle(newItemCommand(MEMBER_A, POST_P, List.of(K1, K2)));

        // then
        Exchange saved = exchangeRepository.findById(exchangeId).orElseThrow();
        assertEquals(1, saved.getOfferedItems().size());
        assertEquals(List.of(finalKey(exchangeId, K1), finalKey(exchangeId, K2)),
                saved.getOfferedItems().get(0).getImageKeys());
        assertEquals(1, exchangeRepository.storedCount());
    }

    @Test
    @DisplayName("사진 확정은 요청자 · 저장된 교환 id · 보낸 key 순서로 한 번 요청한다")
    void handle_newItem_attachesWithRequesterAndSavedExchangeId() {
        Long exchangeId = handler.handle(newItemCommand(MEMBER_A, POST_P, List.of(K2, K1, K3)));

        assertEquals(List.of(new FakeImagePort.Call(MEMBER_A, exchangeId, List.of(K2, K1, K3))), imagePort.calls());
        assertEquals(List.of(finalKey(exchangeId, K2), finalKey(exchangeId, K1), finalKey(exchangeId, K3)),
                savedItem(exchangeId).getImageKeys());
    }

    @Test
    @DisplayName("사진 5장을 보내면 5장 모두 순서대로 저장된다")
    void handle_newItem_fiveImages_savesAll() {
        List<String> keys = List.of(K1, K2, K3,
                "exchange/tmp/101/0b1c2d3e-0004.jpg", "exchange/tmp/101/0b1c2d3e-0005.jpg");

        Long exchangeId = handler.handle(newItemCommand(MEMBER_A, POST_P, keys));

        assertEquals(keys.stream().map(key -> finalKey(exchangeId, key)).toList(),
                savedItem(exchangeId).getImageKeys());
    }

    @Test
    @DisplayName("POST 방식 요청은 사진 확정을 부르지 않고 첨부 아이템 사진은 빈 목록이다")
    void handle_postOffer_doesNotAttachImages() {
        Long exchangeId = handler.handle(new CreateExchangeCommand(MEMBER_A, POST_P, OfferType.POST,
                List.of(POST_Q1), null));

        assertTrue(imagePort.calls().isEmpty());
        assertTrue(savedItem(exchangeId).getImageKeys().isEmpty());
    }

    // ---- AC-2 · AC-11 ----

    @ParameterizedTest
    @ValueSource(strings = {
            OTHER_MEMBER_TMP_KEY,
            "post/tmp/101/0b1c2d3e-0009.jpg",
            "exchange/tmp/101/not-uploaded.jpg",
            "not-a-key",
            "exchange/77/0b1c2d3e-0009.jpg"})
    @DisplayName("image BC가 두 번째 사진을 거부하면 INVALID_IMAGE_KEY이고 메시지에 2번째라고 담는다")
    void handle_secondImageRejected_throwsInvalidImageKey(String rejectedKey) {
        imagePort.reject(rejectedKey);

        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> handler.handle(newItemCommand(MEMBER_A, POST_P, List.of(K1, rejectedKey))));

        assertEquals(ExchangeErrorCode.INVALID_IMAGE_KEY, exception.getErrorCode());
        assertEquals("2번째 이미지를 사용할 수 없습니다.", exception.getMessage());
    }

    @Test
    @DisplayName("첫 번째 사진이 거부되면 메시지에 1번째라고 담는다")
    void handle_firstImageRejected_messageHasFirstOrder() {
        imagePort.reject(OTHER_MEMBER_TMP_KEY);

        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> handler.handle(newItemCommand(MEMBER_A, POST_P, List.of(OTHER_MEMBER_TMP_KEY, K1))));

        assertEquals(ExchangeErrorCode.INVALID_IMAGE_KEY, exception.getErrorCode());
        assertEquals("1번째 이미지를 사용할 수 없습니다.", exception.getMessage());
    }

    @Test
    @DisplayName("판정을 모두 통과하고 두 번째 사진이 거부되면 사진이 들어간 교환은 저장되지 않는다")
    void handle_imageRejectedAfterChecks_neverSavesImages() {
        // given
        imagePort.reject(K2);

        // when
        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> handler.handle(newItemCommand(MEMBER_A, POST_P, List.of(K1, K2))));

        // then: 롤백은 트랜잭션 몫이다. Handler는 예외를 삼키지 않고 사진 반영 저장까지 가지 않는다
        assertEquals(ExchangeErrorCode.INVALID_IMAGE_KEY, exception.getErrorCode());
        assertEquals(1, imagePort.calls().size());
        assertTrue(exchangeRepository.storedExchanges().stream()
                .flatMap(exchange -> exchange.getOfferedItems().stream())
                .allMatch(item -> item.getImageKeys().isEmpty()));
    }

    // ---- AC-3 ----

    @Test
    @DisplayName("없는 글이고 쓸 수 없는 사진이면 POST_NOT_FOUND이고 사진 확정을 부르지 않는다")
    void handle_missingPostWithRejectedImage_postNotFoundFirst() {
        imagePort.reject(OTHER_MEMBER_TMP_KEY);

        assertExistingErrorFirst(ExchangeErrorCode.POST_NOT_FOUND,
                () -> handler.handle(newItemCommand(MEMBER_A, MISSING_POST, List.of(OTHER_MEMBER_TMP_KEY))));
    }

    @Test
    @DisplayName("예약중 글이고 쓸 수 없는 사진이면 POST_RESERVED_ELSEWHERE이고 사진 확정을 부르지 않는다")
    void handle_reservedPostWithRejectedImage_reservedFirst() {
        postPort.register(new PostInfo(POST_P, "P", null, MEMBER_B, PostInfo.Status.RESERVED));
        imagePort.reject(OTHER_MEMBER_TMP_KEY);

        assertExistingErrorFirst(ExchangeErrorCode.POST_RESERVED_ELSEWHERE,
                () -> handler.handle(newItemCommand(MEMBER_A, POST_P, List.of(OTHER_MEMBER_TMP_KEY))));
    }

    @Test
    @DisplayName("교환 완료 글이고 쓸 수 없는 사진이면 POST_NOT_REQUESTABLE이고 사진 확정을 부르지 않는다")
    void handle_completedPostWithRejectedImage_notRequestableFirst() {
        postPort.register(new PostInfo(POST_P, "P", null, MEMBER_B, PostInfo.Status.COMPLETED));
        imagePort.reject(OTHER_MEMBER_TMP_KEY);

        assertExistingErrorFirst(ExchangeErrorCode.POST_NOT_REQUESTABLE,
                () -> handler.handle(newItemCommand(MEMBER_A, POST_P, List.of(OTHER_MEMBER_TMP_KEY))));
    }

    @Test
    @DisplayName("본인 글이고 쓸 수 없는 사진이면 OWN_POST_REQUEST이고 사진 확정을 부르지 않는다")
    void handle_ownPostWithRejectedImage_ownPostFirst() {
        imagePort.reject(OTHER_MEMBER_TMP_KEY);

        assertExistingErrorFirst(ExchangeErrorCode.OWN_POST_REQUEST,
                () -> handler.handle(newItemCommand(MEMBER_B, POST_P, List.of(OTHER_MEMBER_TMP_KEY))));
    }

    @Test
    @DisplayName("차단 관계이고 쓸 수 없는 사진이면 BLOCKED_USER이고 사진 확정을 부르지 않는다")
    void handle_blockedWithRejectedImage_blockedFirst() {
        userPort.block(MEMBER_B, MEMBER_A);
        imagePort.reject(OTHER_MEMBER_TMP_KEY);

        assertExistingErrorFirst(ExchangeErrorCode.BLOCKED_USER,
                () -> handler.handle(newItemCommand(MEMBER_A, POST_P, List.of(OTHER_MEMBER_TMP_KEY))));
    }

    // ---- helpers ----

    private void assertExistingErrorFirst(ExchangeErrorCode expected, Executable action) {
        ExchangeException exception = assertThrows(ExchangeException.class, action);

        assertEquals(expected, exception.getErrorCode());
        assertTrue(imagePort.calls().isEmpty());
        assertEquals(0, exchangeRepository.saveCount());
    }

    private OfferedItem savedItem(Long exchangeId) {
        Exchange saved = exchangeRepository.findById(exchangeId).orElseThrow();
        assertEquals(1, saved.getOfferedItems().size());
        return saved.getOfferedItems().get(0);
    }

    private static String finalKey(Long exchangeId, String tmpKey) {
        return FakeImagePort.toFinalKey(exchangeId, tmpKey);
    }

    private static CreateExchangeCommand newItemCommand(Long requesterId, Long postId, List<String> imageKeys) {
        return new CreateExchangeCommand(requesterId, postId, OfferType.NEW, null,
                new NewItem("치이카와 아크릴 스탠드", false, "가챠에서 두 개 나와서 하나 교환해요", imageKeys));
    }
}
