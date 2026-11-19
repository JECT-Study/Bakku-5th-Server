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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("exchange-update-new-item")
class UpdateNewItemHandlerTest {

    private static final Instant NOW = Instant.parse("2026-11-20T05:30:00Z");
    private static final Long EXCHANGE_ID = 1L;
    private static final Long POST_ID = 10L;
    private static final Long REQUESTER_ID = 100L;
    private static final Long OWNER_ID = 200L;
    private static final Long OTHER_MEMBER_ID = 300L;
    private static final Long ITEM_ID = 31L;
    private static final Long MISSING_EXCHANGE_ID = 999L;
    private static final String ORIGINAL_NAME = "치이카와 아크릴 스탠드";
    private static final String ORIGINAL_DESCRIPTION = "가챠에서 두 개 나와서 하나 교환해요";

    private FakeExchangeRepository exchangeRepository;
    private UpdateNewItemHandler handler;

    @BeforeEach
    void setUp() {
        exchangeRepository = new FakeExchangeRepository();
        handler = new UpdateNewItemHandler(exchangeRepository);
    }

    @Test
    @Tag("AC:exchange-update-new-item:AC-1")
    @DisplayName("요청자가 수정하면 교환 id를 반환하고 같은 아이템 id에 새 값이 저장된다")
    void handle_byRequester_savesNewValues() {
        // given
        exchangeRepository.preload(newItemExchange(ExchangeStatus.ACTIVE));

        // when
        Long result = handler.handle(command(REQUESTER_ID, new NewItem("새 이름", true, "새 설명")));

        // then
        assertEquals(EXCHANGE_ID, result);
        Exchange saved = exchangeRepository.findById(EXCHANGE_ID).orElseThrow();
        assertEquals(OfferType.NEW, saved.getOfferType());
        assertEquals(1, saved.getOfferedItems().size());
        OfferedItem item = saved.getOfferedItems().get(0);
        assertEquals(ITEM_ID, item.getId());
        assertEquals("새 이름", item.getName().value());
        assertEquals(true, item.getOpened());
        assertEquals("새 설명", item.getDescription().value());
        assertEquals(1, exchangeRepository.saveCount());
    }

    @Test
    @Tag("AC:exchange-update-new-item:AC-2")
    @DisplayName("설명 없이 수정하면 저장된 설명이 비어 있다")
    void handle_withoutDescription_savesNullDescription() {
        exchangeRepository.preload(newItemExchange(ExchangeStatus.ACTIVE));

        handler.handle(command(REQUESTER_ID, new NewItem("새 이름", true, null)));

        OfferedItem item = savedItem();
        assertEquals("새 이름", item.getName().value());
        assertNull(descriptionValue(item));
    }

    @Test
    @Tag("AC:exchange-update-new-item:AC-3")
    @DisplayName("설명 1,000자로 수정하면 저장된 설명 길이가 1,000이다")
    void handle_thousandCharacterDescription_saves() {
        exchangeRepository.preload(newItemExchange(ExchangeStatus.ACTIVE));

        handler.handle(command(REQUESTER_ID, new NewItem("새 이름", false, "가".repeat(1000))));

        assertEquals(1000, savedItem().getDescription().value().length());
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 40})
    @Tag("AC:exchange-update-new-item:AC-4")
    @DisplayName("제품명 2자 · 40자로 수정하면 입력과 같은 제품명이 저장된다")
    void handle_boundaryNameLength_saves(int length) {
        exchangeRepository.preload(newItemExchange(ExchangeStatus.ACTIVE));
        String name = "가".repeat(length);

        handler.handle(command(REQUESTER_ID, new NewItem(name, false, null)));

        assertEquals(name, savedItem().getName().value());
    }

    @Test
    @Tag("AC:exchange-update-new-item:AC-5")
    @Tag("AC:exchange-update-new-item:AC-15")
    @DisplayName("없는 교환이면 EXCHANGE_NOT_FOUND이고 아무것도 저장하지 않는다")
    void handle_missingExchange_throwsExchangeNotFound() {
        exchangeRepository.preload(newItemExchange(ExchangeStatus.ACTIVE));

        ExchangeException exception = assertThrows(ExchangeException.class, () -> handler.handle(
                new UpdateNewItemCommand(MISSING_EXCHANGE_ID, REQUESTER_ID, new NewItem("새 이름", true, "새 설명"))));

        assertEquals(ExchangeErrorCode.EXCHANGE_NOT_FOUND, exception.getErrorCode());
        assertEquals(0, exchangeRepository.saveCount());
        assertStoredItemUnchanged();
    }

    @Test
    @Tag("AC:exchange-update-new-item:AC-6")
    @Tag("AC:exchange-update-new-item:AC-15")
    @DisplayName("게시자가 수정하면 NOT_REQUESTER이고 저장값은 그대로다")
    void handle_byOwner_throwsNotRequester() {
        exchangeRepository.preload(newItemExchange(ExchangeStatus.ACTIVE));

        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> handler.handle(command(OWNER_ID, new NewItem("새 이름", true, "새 설명"))));

        assertEquals(ExchangeErrorCode.NOT_REQUESTER, exception.getErrorCode());
        assertEquals(0, exchangeRepository.saveCount());
        assertStoredItemUnchanged();
    }

    @Test
    @Tag("AC:exchange-update-new-item:AC-7")
    @Tag("AC:exchange-update-new-item:AC-15")
    @DisplayName("교환 참여자가 아닌 회원이 수정하면 NOT_REQUESTER이고 저장값은 그대로다")
    void handle_byThirdParty_throwsNotRequester() {
        exchangeRepository.preload(newItemExchange(ExchangeStatus.ACTIVE));

        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> handler.handle(command(OTHER_MEMBER_ID, new NewItem("새 이름", true, "새 설명"))));

        assertEquals(ExchangeErrorCode.NOT_REQUESTER, exception.getErrorCode());
        assertEquals(0, exchangeRepository.saveCount());
        assertStoredItemUnchanged();
    }

    @Test
    @Tag("AC:exchange-update-new-item:AC-8")
    @Tag("AC:exchange-update-new-item:AC-15")
    @DisplayName("POST 방식 교환이면 NOT_NEW_ITEM_OFFER이고 저장값은 그대로다")
    void handle_postOffer_throwsNotNewItemOffer() {
        exchangeRepository.preload(Exchange.of(EXCHANGE_ID, POST_ID, REQUESTER_ID, OWNER_ID, OfferType.POST,
                List.of(OfferedItem.of(ITEM_ID, 501L, null, null, null, 0)),
                ExchangeStatus.ACTIVE, null, NOW, NOW));

        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> handler.handle(command(REQUESTER_ID, new NewItem("새 이름", true, "새 설명"))));

        assertEquals(ExchangeErrorCode.NOT_NEW_ITEM_OFFER, exception.getErrorCode());
        assertEquals(0, exchangeRepository.saveCount());
        Exchange stored = exchangeRepository.findById(EXCHANGE_ID).orElseThrow();
        assertEquals(OfferType.POST, stored.getOfferType());
        assertEquals(501L, stored.getOfferedItems().get(0).getSourcePostId());
    }

    @ParameterizedTest
    @EnumSource(value = ExchangeStatus.class, names = {"COMPLETED", "CLOSED", "LEFT"})
    @Tag("AC:exchange-update-new-item:AC-9")
    @Tag("AC:exchange-update-new-item:AC-15")
    @DisplayName("종료된(COMPLETED · CLOSED · LEFT) 교환이면 EXCHANGE_CLOSED이고 저장값은 그대로다")
    void handle_notActive_throwsExchangeClosed(ExchangeStatus status) {
        exchangeRepository.preload(newItemExchange(status));

        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> handler.handle(command(REQUESTER_ID, new NewItem("새 이름", true, "새 설명"))));

        assertEquals(ExchangeErrorCode.EXCHANGE_CLOSED, exception.getErrorCode());
        assertEquals(0, exchangeRepository.saveCount());
        assertStoredItemUnchanged();
        assertEquals(status, exchangeRepository.findById(EXCHANGE_ID).orElseThrow().getStatus());
    }

    @Test
    @Tag("AC:exchange-update-new-item:AC-10")
    @Tag("AC:exchange-update-new-item:AC-15")
    @DisplayName("제품명 규칙을 어기면 INVALID_OFFERED_ITEMS이고 저장값은 그대로다")
    void handle_invalidName_throwsInvalidOfferedItems() {
        exchangeRepository.preload(newItemExchange(ExchangeStatus.ACTIVE));

        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> handler.handle(command(REQUESTER_ID, new NewItem("가".repeat(41), true, "새 설명"))));

        assertEquals(ExchangeErrorCode.INVALID_OFFERED_ITEMS, exception.getErrorCode());
        assertEquals(0, exchangeRepository.saveCount());
        assertStoredItemUnchanged();
    }

    @Test
    @Tag("AC:exchange-update-new-item:AC-11")
    @Tag("AC:exchange-update-new-item:AC-15")
    @DisplayName("설명 1,001자면 INVALID_OFFERED_ITEMS이고 저장값은 그대로다")
    void handle_tooLongDescription_throwsInvalidOfferedItems() {
        exchangeRepository.preload(newItemExchange(ExchangeStatus.ACTIVE));

        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> handler.handle(command(REQUESTER_ID, new NewItem("새 이름", true, "가".repeat(1001)))));

        assertEquals(ExchangeErrorCode.INVALID_OFFERED_ITEMS, exception.getErrorCode());
        assertEquals(0, exchangeRepository.saveCount());
        assertStoredItemUnchanged();
    }

    private static UpdateNewItemCommand command(Long memberId, NewItem newItem) {
        return new UpdateNewItemCommand(EXCHANGE_ID, memberId, newItem);
    }

    private static Exchange newItemExchange(ExchangeStatus status) {
        Instant completedAt = status == ExchangeStatus.COMPLETED ? NOW : null;
        return Exchange.of(EXCHANGE_ID, POST_ID, REQUESTER_ID, OWNER_ID, OfferType.NEW,
                List.of(OfferedItem.of(ITEM_ID, null, new ItemName(ORIGINAL_NAME), false,
                        new ItemDescription(ORIGINAL_DESCRIPTION), 0)),
                status, completedAt, NOW, NOW);
    }

    private OfferedItem savedItem() {
        Exchange saved = exchangeRepository.findById(EXCHANGE_ID).orElseThrow();
        assertEquals(1, saved.getOfferedItems().size());
        OfferedItem item = saved.getOfferedItems().get(0);
        assertEquals(ITEM_ID, item.getId());
        return item;
    }

    private void assertStoredItemUnchanged() {
        OfferedItem item = savedItem();
        assertEquals(ORIGINAL_NAME, item.getName().value());
        assertEquals(false, item.getOpened());
        assertEquals(ORIGINAL_DESCRIPTION, item.getDescription().value());
    }

    private static String descriptionValue(OfferedItem item) {
        return item.getDescription() == null ? null : item.getDescription().value();
    }
}
