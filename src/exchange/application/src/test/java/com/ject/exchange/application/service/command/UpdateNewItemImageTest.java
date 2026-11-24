package com.ject.exchange.application.service.command;

import com.ject.exchange.application.dto.command.UpdateNewItemCommand;
import com.ject.exchange.application.dto.dto.NewItem;
import com.ject.exchange.domain.domain.exchange.Exchange;
import com.ject.exchange.domain.domain.exchange.ExchangeStatus;
import com.ject.exchange.domain.domain.exchange.OfferType;
import com.ject.exchange.domain.domain.exchange.OfferedItem;
import com.ject.exchange.domain.domain.exchange.vo.ItemDescription;
import com.ject.exchange.domain.domain.exchange.vo.ItemName;
import com.ject.exchange.domain.exception.ExchangeErrorCode;
import com.ject.exchange.domain.exception.ExchangeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E-3 새로 작성한 아이템 수정이 사진 목록을 요청 순서대로 바꾸는지 확인한다 (#108).
 */
class UpdateNewItemImageTest {

    private static final Instant NOW = Instant.parse("2026-11-23T05:00:00Z");
    private static final Long EXCHANGE_ID = 1L;
    private static final Long POST_P = 10L;
    private static final Long MEMBER_A = 101L;
    private static final Long MEMBER_B = 102L;
    private static final Long MEMBER_C = 103L;
    private static final Long ITEM_ID = 31L;
    private static final Long MISSING_EXCHANGE_ID = 999L;
    private static final String ORIGINAL_NAME = "치이카와 아크릴 스탠드";
    private static final String ORIGINAL_DESCRIPTION = "가챠에서 두 개 나와서 하나 교환해요";
    private static final String K1_FINAL = "exchange/1/0b1c2d3e-0001.jpg";
    private static final String K2_FINAL = "exchange/1/0b1c2d3e-0002.jpg";
    private static final String K3 = "exchange/tmp/101/0b1c2d3e-0003.jpg";
    private static final String K3_FINAL = "exchange/1/0b1c2d3e-0003.jpg";
    private static final String OTHER_MEMBER_TMP_KEY = "exchange/tmp/103/0b1c2d3e-0009.jpg";

    private FakeExchangeRepository exchangeRepository;
    private FakeImagePort imagePort;
    private UpdateNewItemHandler handler;

    @BeforeEach
    void setUp() {
        exchangeRepository = new FakeExchangeRepository();
        imagePort = new FakeImagePort();
        FakeAppointmentRepository appointmentRepository = new FakeAppointmentRepository(exchangeRepository, NOW);
        handler = new UpdateNewItemHandler(exchangeRepository, appointmentRepository,
                new FakePostLockRepository(appointmentRepository), imagePort);
    }

    // ---- AC-5 ----

    @Test
    @DisplayName("사진 [K1, K2]인 교환을 [K2 정식, K3]로 바꾸면 저장된 사진은 [K2 정식, K3 정식]이고 K1은 빠진다")
    void handle_replaceImages_savesNewOrder() {
        // given
        exchangeRepository.preload(newItemExchange(ExchangeStatus.ACTIVE));

        // when
        Long result = handler.handle(command(MEMBER_A, newItem(List.of(K2_FINAL, K3))));

        // then
        assertEquals(EXCHANGE_ID, result);
        OfferedItem item = storedItem();
        assertEquals(ITEM_ID, item.getId());
        assertEquals(List.of(K2_FINAL, K3_FINAL), item.getImageKeys());
        assertEquals("새 이름", item.getName().value());
        assertEquals(true, item.getOpened());
        assertEquals("새 설명", item.getDescription().value());
    }

    @Test
    @DisplayName("사진 확정은 요청자 · 경로의 교환 id · 보낸 key 순서로 한 번 요청한다")
    void handle_replaceImages_attachesWithRequesterAndExchangeId() {
        exchangeRepository.preload(newItemExchange(ExchangeStatus.ACTIVE));

        handler.handle(command(MEMBER_A, newItem(List.of(K2_FINAL, K3))));

        assertEquals(List.of(new FakeImagePort.Call(MEMBER_A, EXCHANGE_ID, List.of(K2_FINAL, K3))),
                imagePort.calls());
    }

    @Test
    @DisplayName("정식 key만 순서를 바꿔 보내면 그 순서로 저장된다")
    void handle_reorderFinalKeys_savesNewOrder() {
        exchangeRepository.preload(newItemExchange(ExchangeStatus.ACTIVE));

        handler.handle(command(MEMBER_A, newItem(List.of(K2_FINAL, K1_FINAL))));

        assertEquals(List.of(K2_FINAL, K1_FINAL), storedItem().getImageKeys());
    }

    // ---- AC-6 ----

    @ParameterizedTest
    @ValueSource(strings = {
            OTHER_MEMBER_TMP_KEY,
            "post/tmp/101/0b1c2d3e-0009.jpg",
            "exchange/tmp/101/not-uploaded.jpg",
            "not-a-key",
            "exchange/77/0b1c2d3e-0009.jpg"})
    @DisplayName("두 번째 사진이 거부되면 INVALID_IMAGE_KEY이고 제품명 · 개봉 여부 · 설명 · 사진 모두 그대로다")
    void handle_imageRejected_keepsEverything(String rejectedKey) {
        // given
        exchangeRepository.preload(newItemExchange(ExchangeStatus.ACTIVE));
        imagePort.reject(rejectedKey);

        // when
        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> handler.handle(command(MEMBER_A, newItem(List.of(K2_FINAL, rejectedKey)))));

        // then
        assertEquals(ExchangeErrorCode.INVALID_IMAGE_KEY, exception.getErrorCode());
        assertEquals("2번째 이미지를 사용할 수 없습니다.", exception.getMessage());
        assertEquals(0, exchangeRepository.saveCount());
        assertStoredUnchanged();
    }

    // ---- AC-7 ----

    @Test
    @DisplayName("없는 교환이고 쓸 수 없는 사진이면 EXCHANGE_NOT_FOUND이고 사진 확정을 부르지 않는다")
    void handle_missingExchangeWithRejectedImage_notFoundFirst() {
        exchangeRepository.preload(newItemExchange(ExchangeStatus.ACTIVE));
        imagePort.reject(OTHER_MEMBER_TMP_KEY);

        assertExistingErrorFirst(ExchangeErrorCode.EXCHANGE_NOT_FOUND, () -> handler.handle(new UpdateNewItemCommand(
                MISSING_EXCHANGE_ID, MEMBER_A, newItem(List.of(OTHER_MEMBER_TMP_KEY)))));
    }

    @Test
    @DisplayName("게시자가 쓸 수 없는 사진으로 수정하면 NOT_REQUESTER이고 사진 확정을 부르지 않는다")
    void handle_byOwnerWithRejectedImage_notRequesterFirst() {
        exchangeRepository.preload(newItemExchange(ExchangeStatus.ACTIVE));
        imagePort.reject(OTHER_MEMBER_TMP_KEY);

        assertExistingErrorFirst(ExchangeErrorCode.NOT_REQUESTER,
                () -> handler.handle(command(MEMBER_B, newItem(List.of(OTHER_MEMBER_TMP_KEY)))));
        assertStoredUnchanged();
    }

    @Test
    @DisplayName("참여자가 아닌 회원 C가 수정하면 NOT_REQUESTER이고 사진 확정을 부르지 않는다")
    void handle_byThirdParty_notRequester() {
        exchangeRepository.preload(newItemExchange(ExchangeStatus.ACTIVE));

        assertExistingErrorFirst(ExchangeErrorCode.NOT_REQUESTER,
                () -> handler.handle(command(MEMBER_C, newItem(List.of(K3)))));
        assertStoredUnchanged();
    }

    @Test
    @DisplayName("POST 방식 교환이고 쓸 수 없는 사진이면 NOT_NEW_ITEM_OFFER이고 사진 확정을 부르지 않는다")
    void handle_postOfferWithRejectedImage_notNewItemOfferFirst() {
        exchangeRepository.preload(Exchange.of(EXCHANGE_ID, POST_P, MEMBER_A, MEMBER_B, OfferType.POST,
                List.of(OfferedItem.of(ITEM_ID, 501L, null, null, null, 0)),
                ExchangeStatus.ACTIVE, null, NOW, NOW));
        imagePort.reject(OTHER_MEMBER_TMP_KEY);

        assertExistingErrorFirst(ExchangeErrorCode.NOT_NEW_ITEM_OFFER,
                () -> handler.handle(command(MEMBER_A, newItem(List.of(OTHER_MEMBER_TMP_KEY)))));
    }

    @ParameterizedTest
    @EnumSource(value = ExchangeStatus.class, names = {"COMPLETED", "CLOSED", "LEFT"})
    @DisplayName("종료된 교환이고 쓸 수 없는 사진이면 EXCHANGE_CLOSED이고 사진 확정을 부르지 않는다")
    void handle_closedWithRejectedImage_closedFirst(ExchangeStatus status) {
        exchangeRepository.preload(newItemExchange(status));
        imagePort.reject(OTHER_MEMBER_TMP_KEY);

        assertExistingErrorFirst(ExchangeErrorCode.EXCHANGE_CLOSED,
                () -> handler.handle(command(MEMBER_A, newItem(List.of(OTHER_MEMBER_TMP_KEY)))));
        assertStoredUnchanged();
    }

    // ---- helpers ----

    private void assertExistingErrorFirst(ExchangeErrorCode expected, Executable action) {
        ExchangeException exception = assertThrows(ExchangeException.class, action);

        assertEquals(expected, exception.getErrorCode());
        assertTrue(imagePort.calls().isEmpty());
        assertEquals(0, exchangeRepository.saveCount());
    }

    private void assertStoredUnchanged() {
        OfferedItem item = storedItem();
        assertEquals(ORIGINAL_NAME, item.getName().value());
        assertEquals(false, item.getOpened());
        assertEquals(ORIGINAL_DESCRIPTION, item.getDescription().value());
        assertEquals(List.of(K1_FINAL, K2_FINAL), item.getImageKeys());
    }

    private OfferedItem storedItem() {
        Exchange stored = exchangeRepository.findById(EXCHANGE_ID).orElseThrow();
        assertEquals(1, stored.getOfferedItems().size());
        return stored.getOfferedItems().get(0);
    }

    private static UpdateNewItemCommand command(Long memberId, NewItem newItem) {
        return new UpdateNewItemCommand(EXCHANGE_ID, memberId, newItem);
    }

    private static NewItem newItem(List<String> imageKeys) {
        return new NewItem("새 이름", true, "새 설명", imageKeys);
    }

    /** AC-1의 교환: 사진 K1 · K2 정식 */
    private static Exchange newItemExchange(ExchangeStatus status) {
        Instant completedAt = status == ExchangeStatus.COMPLETED ? NOW : null;
        return Exchange.of(EXCHANGE_ID, POST_P, MEMBER_A, MEMBER_B, OfferType.NEW,
                List.of(OfferedItem.of(ITEM_ID, null, new ItemName(ORIGINAL_NAME), false,
                        new ItemDescription(ORIGINAL_DESCRIPTION), 0, List.of(K1_FINAL, K2_FINAL))),
                status, completedAt, NOW, NOW);
    }
}
