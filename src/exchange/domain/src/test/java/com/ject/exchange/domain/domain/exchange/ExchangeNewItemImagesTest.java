package com.ject.exchange.domain.domain.exchange;

import com.ject.exchange.domain.domain.exchange.vo.ItemDescription;
import com.ject.exchange.domain.domain.exchange.vo.ItemName;
import com.ject.exchange.domain.exception.ExchangeErrorCode;
import com.ject.exchange.domain.exception.ExchangeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 새로 작성한 아이템의 사진 key 목록 규칙을 확인한다 (#108).
 */
class ExchangeNewItemImagesTest {

    private static final Instant NOW = Instant.parse("2026-11-23T05:00:00Z");
    private static final Long EXCHANGE_ID = 1L;
    private static final Long POST_ID = 10L;
    private static final Long REQUESTER_ID = 101L;
    private static final Long OWNER_ID = 102L;
    private static final Long ITEM_ID = 31L;
    private static final String NAME = "치이카와 아크릴 스탠드";
    private static final String DESCRIPTION = "가챠에서 두 개 나와서 하나 교환해요";
    private static final String K1_FINAL = "exchange/1/0b1c2d3e-0001.jpg";
    private static final String K2_FINAL = "exchange/1/0b1c2d3e-0002.jpg";
    private static final String K3_FINAL = "exchange/1/0b1c2d3e-0003.jpg";

    // ---- OfferedItem ----

    @Test
    @DisplayName("사진 key 없이 만든 아이템의 사진 목록은 빈 목록이다")
    void offeredItem_withoutImageKeys_hasEmptyList() {
        OfferedItem created = OfferedItem.ofNew(new ItemName(NAME), false, new ItemDescription(DESCRIPTION), 0);
        OfferedItem loaded = OfferedItem.of(ITEM_ID, null, new ItemName(NAME), false,
                new ItemDescription(DESCRIPTION), 0);

        assertTrue(created.getImageKeys().isEmpty());
        assertTrue(loaded.getImageKeys().isEmpty());
    }

    @Test
    @DisplayName("POST 아이템의 사진 목록은 빈 목록이다")
    void offeredItem_postItem_hasEmptyList() {
        OfferedItem item = OfferedItem.of(ITEM_ID, 501L, null, null, null, 0);

        assertTrue(item.getImageKeys().isEmpty());
    }

    @Test
    @DisplayName("사진 key와 함께 만든 아이템은 그 순서대로 사진 목록을 가진다")
    void offeredItem_withImageKeys_keepsOrder() {
        OfferedItem item = newItem(List.of(K2_FINAL, K1_FINAL));

        assertEquals(List.of(K2_FINAL, K1_FINAL), item.getImageKeys());
    }

    @Test
    @DisplayName("withImageKeys는 사진 목록만 바꾸고 id · 제품명 · 개봉 여부 · 설명 · 순서는 그대로다")
    void offeredItem_withImageKeys_replacesOnlyImages() {
        OfferedItem item = newItem(List.of(K1_FINAL, K2_FINAL));

        OfferedItem changed = item.withImageKeys(List.of(K2_FINAL, K3_FINAL));

        assertEquals(List.of(K2_FINAL, K3_FINAL), changed.getImageKeys());
        assertEquals(ITEM_ID, changed.getId());
        assertEquals(NAME, changed.getName().value());
        assertEquals(false, changed.getOpened());
        assertEquals(DESCRIPTION, changed.getDescription().value());
        assertEquals(0, changed.getSortOrder());
    }

    @Test
    @DisplayName("updateNew로 제품명 · 개봉 여부 · 설명을 바꿔도 사진 목록은 그대로다")
    void offeredItem_updateNew_keepsImages() {
        OfferedItem item = newItem(List.of(K1_FINAL, K2_FINAL));

        OfferedItem updated = item.updateNew(new ItemName("새 이름"), true, new ItemDescription("새 설명"));

        assertEquals(List.of(K1_FINAL, K2_FINAL), updated.getImageKeys());
    }

    // ---- Exchange ----

    @Test
    @DisplayName("withNewItemImages는 NEW 아이템의 사진만 새 순서로 바꾼 교환을 돌려준다")
    void exchange_withNewItemImages_replacesNewItemImages() {
        Exchange exchange = newItemExchange(List.of(K1_FINAL, K2_FINAL));

        Exchange changed = exchange.withNewItemImages(List.of(K2_FINAL, K3_FINAL));

        assertEquals(EXCHANGE_ID, changed.getId());
        assertEquals(1, changed.getOfferedItems().size());
        OfferedItem item = changed.getOfferedItems().get(0);
        assertEquals(ITEM_ID, item.getId());
        assertEquals(List.of(K2_FINAL, K3_FINAL), item.getImageKeys());
        assertEquals(NAME, item.getName().value());
        assertEquals(DESCRIPTION, item.getDescription().value());
    }

    @Test
    @DisplayName("사진 없는 NEW 교환에 withNewItemImages를 하면 사진이 그 순서로 들어간다")
    void exchange_withNewItemImages_onEmpty_setsImages() {
        Exchange exchange = newItemExchange(List.of());

        Exchange changed = exchange.withNewItemImages(List.of(K1_FINAL, K2_FINAL));

        assertEquals(List.of(K1_FINAL, K2_FINAL), changed.getOfferedItems().get(0).getImageKeys());
    }

    @Test
    @DisplayName("POST 방식 교환에 withNewItemImages를 하면 NOT_NEW_ITEM_OFFER다")
    void exchange_withNewItemImages_postOffer_throwsNotNewItemOffer() {
        Exchange exchange = Exchange.of(EXCHANGE_ID, POST_ID, REQUESTER_ID, OWNER_ID, OfferType.POST,
                List.of(OfferedItem.of(ITEM_ID, 501L, null, null, null, 0)),
                ExchangeStatus.ACTIVE, null, NOW, NOW);

        ExchangeException exception = assertThrows(ExchangeException.class,
                () -> exchange.withNewItemImages(List.of(K1_FINAL)));

        assertEquals(ExchangeErrorCode.NOT_NEW_ITEM_OFFER, exception.getErrorCode());
    }

    @Test
    @DisplayName("updateNewItem으로 아이템 값을 바꿔도 사진 목록은 그대로다")
    void exchange_updateNewItem_keepsImages() {
        Exchange exchange = newItemExchange(List.of(K1_FINAL, K2_FINAL));

        Exchange updated = exchange.updateNewItem(REQUESTER_ID, new ItemName("새 이름"), true,
                new ItemDescription("새 설명"));

        assertEquals(List.of(K1_FINAL, K2_FINAL), updated.getOfferedItems().get(0).getImageKeys());
    }

    @Test
    @DisplayName("ExchangeException에 메시지를 주면 에러 코드는 INVALID_IMAGE_KEY이고 메시지는 준 값이다")
    void exchangeException_withMessage_keepsCodeAndMessage() {
        ExchangeException exception = new ExchangeException(ExchangeErrorCode.INVALID_IMAGE_KEY,
                "2번째 이미지를 사용할 수 없습니다.");

        assertEquals(ExchangeErrorCode.INVALID_IMAGE_KEY, exception.getErrorCode());
        assertEquals("2번째 이미지를 사용할 수 없습니다.", exception.getMessage());
        assertEquals("EXCHANGE_015", ExchangeErrorCode.INVALID_IMAGE_KEY.getCode());
        assertEquals(400, ExchangeErrorCode.INVALID_IMAGE_KEY.getStatus());
    }

    private static OfferedItem newItem(List<String> imageKeys) {
        return OfferedItem.of(ITEM_ID, null, new ItemName(NAME), false, new ItemDescription(DESCRIPTION), 0,
                imageKeys);
    }

    private static Exchange newItemExchange(List<String> imageKeys) {
        return Exchange.of(EXCHANGE_ID, POST_ID, REQUESTER_ID, OWNER_ID, OfferType.NEW,
                List.of(newItem(imageKeys)), ExchangeStatus.ACTIVE, null, NOW, NOW);
    }
}
