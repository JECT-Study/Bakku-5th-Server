package com.ject.exchange.domain.domain.exchange;

import com.ject.exchange.domain.domain.exchange.vo.ItemDescription;
import com.ject.exchange.domain.domain.exchange.vo.ItemName;
import com.ject.exchange.domain.exception.ExchangeErrorCode;
import com.ject.exchange.domain.exception.ExchangeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
class ExchangeTest {

    private static final Instant NOW = Instant.parse("2026-11-20T05:30:00Z");
    private static final Long EXCHANGE_ID = 1L;
    private static final Long POST_ID = 10L;
    private static final Long REQUESTER_ID = 100L;
    private static final Long OWNER_ID = 200L;
    private static final Long OTHER_MEMBER_ID = 300L;
    private static final Long ITEM_ID = 31L;
    private static final String ORIGINAL_NAME = "치이카와 아크릴 스탠드";
    private static final String ORIGINAL_DESCRIPTION = "가챠에서 두 개 나와서 하나 교환해요";

    @Test
    @DisplayName("요청자가 NEW · ACTIVE 교환의 아이템을 수정하면 아이템 id는 그대로이고 값만 바뀐다")
    void updateNewItem_byRequester_changesValues() {
        // given
        Exchange exchange = newItemExchange(ExchangeStatus.ACTIVE);

        // when
        Exchange updated = exchange.updateNewItem(REQUESTER_ID, new ItemName("새 이름"), true,
                new ItemDescription("새 설명"));

        // then
        assertEquals(EXCHANGE_ID, updated.getId());
        assertEquals(OfferType.NEW, updated.getOfferType());
        assertEquals(1, updated.getOfferedItems().size());
        OfferedItem item = updated.getOfferedItems().get(0);
        assertEquals(ITEM_ID, item.getId());
        assertEquals("새 이름", item.getName().value());
        assertEquals(true, item.getOpened());
        assertEquals("새 설명", item.getDescription().value());
    }

    @Test
    @DisplayName("설명 없이 수정하면 아이템 설명이 비어 있다")
    void updateNewItem_withoutDescription_clearsDescription() {
        Exchange exchange = newItemExchange(ExchangeStatus.ACTIVE);

        Exchange updated = exchange.updateNewItem(REQUESTER_ID, new ItemName("새 이름"), true,
                new ItemDescription(null));

        assertNull(descriptionValue(updated.getOfferedItems().get(0)));
    }

    @Test
    @DisplayName("수정은 새 객체를 반환하고 원본 교환의 아이템은 바뀌지 않는다")
    void updateNewItem_keepsOriginalUnchanged() {
        Exchange exchange = newItemExchange(ExchangeStatus.ACTIVE);

        exchange.updateNewItem(REQUESTER_ID, new ItemName("새 이름"), true, new ItemDescription("새 설명"));

        assertOriginalItem(exchange);
    }

    @Test
    @DisplayName("게시자가 수정하면 NOT_REQUESTER")
    void updateNewItem_byOwner_throwsNotRequester() {
        Exchange exchange = newItemExchange(ExchangeStatus.ACTIVE);

        ExchangeException exception = assertThrows(ExchangeException.class, () -> exchange.updateNewItem(OWNER_ID,
                new ItemName("새 이름"), true, new ItemDescription("새 설명")));

        assertEquals(ExchangeErrorCode.NOT_REQUESTER, exception.getErrorCode());
        assertOriginalItem(exchange);
    }

    @Test
    @DisplayName("교환 참여자가 아닌 회원이 수정하면 NOT_REQUESTER")
    void updateNewItem_byThirdParty_throwsNotRequester() {
        Exchange exchange = newItemExchange(ExchangeStatus.ACTIVE);

        ExchangeException exception = assertThrows(ExchangeException.class, () -> exchange.updateNewItem(
                OTHER_MEMBER_ID, new ItemName("새 이름"), true, new ItemDescription("새 설명")));

        assertEquals(ExchangeErrorCode.NOT_REQUESTER, exception.getErrorCode());
        assertOriginalItem(exchange);
    }

    @Test
    @DisplayName("POST 방식 교환을 요청자가 수정하면 NOT_NEW_ITEM_OFFER")
    void updateNewItem_postOffer_throwsNotNewItemOffer() {
        Exchange exchange = Exchange.of(EXCHANGE_ID, POST_ID, REQUESTER_ID, OWNER_ID, OfferType.POST,
                List.of(OfferedItem.of(ITEM_ID, 501L, null, null, null, 0)),
                ExchangeStatus.ACTIVE, null, NOW, NOW);

        ExchangeException exception = assertThrows(ExchangeException.class, () -> exchange.updateNewItem(
                REQUESTER_ID, new ItemName("새 이름"), true, new ItemDescription("새 설명")));

        assertEquals(ExchangeErrorCode.NOT_NEW_ITEM_OFFER, exception.getErrorCode());
        assertEquals(501L, exchange.getOfferedItems().get(0).getSourcePostId());
    }

    @ParameterizedTest
    @EnumSource(value = ExchangeStatus.class, names = {"COMPLETED", "CLOSED", "LEFT"})
    @DisplayName("종료된(COMPLETED · CLOSED · LEFT) 교환을 요청자가 수정하면 EXCHANGE_CLOSED")
    void updateNewItem_notActive_throwsExchangeClosed(ExchangeStatus status) {
        Exchange exchange = newItemExchange(status);

        ExchangeException exception = assertThrows(ExchangeException.class, () -> exchange.updateNewItem(
                REQUESTER_ID, new ItemName("새 이름"), true, new ItemDescription("새 설명")));

        assertEquals(ExchangeErrorCode.EXCHANGE_CLOSED, exception.getErrorCode());
        assertOriginalItem(exchange);
    }

    @Test
    @DisplayName("게시자가 종료된 교환을 수정해도 요청자 판정이 먼저라 NOT_REQUESTER")
    void updateNewItem_byOwnerOnClosed_throwsNotRequesterFirst() {
        Exchange exchange = newItemExchange(ExchangeStatus.CLOSED);

        ExchangeException exception = assertThrows(ExchangeException.class, () -> exchange.updateNewItem(OWNER_ID,
                new ItemName("새 이름"), true, new ItemDescription("새 설명")));

        assertEquals(ExchangeErrorCode.NOT_REQUESTER, exception.getErrorCode());
    }

    @Test
    @DisplayName("POST 방식이면서 종료된 교환은 방식 판정이 먼저라 NOT_NEW_ITEM_OFFER")
    void updateNewItem_postOfferOnClosed_throwsNotNewItemOfferFirst() {
        Exchange exchange = Exchange.of(EXCHANGE_ID, POST_ID, REQUESTER_ID, OWNER_ID, OfferType.POST,
                List.of(OfferedItem.of(ITEM_ID, 501L, null, null, null, 0)),
                ExchangeStatus.CLOSED, null, NOW, NOW);

        ExchangeException exception = assertThrows(ExchangeException.class, () -> exchange.updateNewItem(
                REQUESTER_ID, new ItemName("새 이름"), true, new ItemDescription("새 설명")));

        assertEquals(ExchangeErrorCode.NOT_NEW_ITEM_OFFER, exception.getErrorCode());
    }

    private static Exchange newItemExchange(ExchangeStatus status) {
        Instant completedAt = status == ExchangeStatus.COMPLETED ? NOW : null;
        return Exchange.of(EXCHANGE_ID, POST_ID, REQUESTER_ID, OWNER_ID, OfferType.NEW,
                List.of(OfferedItem.of(ITEM_ID, null, new ItemName(ORIGINAL_NAME), false,
                        new ItemDescription(ORIGINAL_DESCRIPTION), 0)),
                status, completedAt, NOW, NOW);
    }

    private static void assertOriginalItem(Exchange exchange) {
        assertEquals(1, exchange.getOfferedItems().size());
        OfferedItem item = exchange.getOfferedItems().get(0);
        assertEquals(ITEM_ID, item.getId());
        assertEquals(ORIGINAL_NAME, item.getName().value());
        assertEquals(false, item.getOpened());
        assertEquals(ORIGINAL_DESCRIPTION, item.getDescription().value());
    }

    private static String descriptionValue(OfferedItem item) {
        return item.getDescription() == null ? null : item.getDescription().value();
    }
}
