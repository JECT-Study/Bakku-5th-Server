package com.ject.exchange.application.dto.dto;

import com.ject.exchange.domain.domain.exchange.Exchange;
import com.ject.exchange.domain.domain.exchange.ExchangeStatus;
import com.ject.exchange.domain.domain.exchange.OfferType;
import com.ject.exchange.domain.domain.exchange.OfferedItem;
import com.ject.exchange.domain.domain.exchange.vo.ItemDescription;
import com.ject.exchange.domain.domain.exchange.vo.ItemName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 조회자 · 버튼 · 잠금을 받는 {@code ExchangeResult.of} 오버로드가 {@code editable}과 버튼을 채우는지 확인한다 (#120).
 */
class ExchangeResultEditableTest {

    private static final Instant NOW = Instant.parse("2026-11-24T05:00:00Z");
    private static final Long EXCHANGE_ID = 1L;
    private static final Long POST_ID = 10L;
    private static final Long SOURCE_POST_ID = 501L;
    private static final Long REQUESTER_ID = 100L;
    private static final Long OWNER_ID = 200L;
    private static final Long ITEM_ID = 31L;

    private static final Map<Long, PostInfo> POSTS = Map.of(
            POST_ID, new PostInfo(POST_ID, "치이카와 교환해요", null, OWNER_ID, PostInfo.Status.AVAILABLE),
            SOURCE_POST_ID, new PostInfo(SOURCE_POST_ID, "우사기 미니 피규어", null, REQUESTER_ID,
                    PostInfo.Status.AVAILABLE));
    private static final Map<Long, UserInfo> USERS = Map.of(
            REQUESTER_ID, new UserInfo(REQUESTER_ID, "바꾸", 0),
            OWNER_ID, new UserInfo(OWNER_ID, "하치", 0));

    // ---- AC-12 ----

    @Test
    @DisplayName("요청자가 보는 NEW · ACTIVE 교환이고 잠금이 없으면 새로 작성한 제품 editable true")
    void of_requesterUnlockedNewItem_isEditable() {
        ExchangeResult result = ExchangeResult.of(newItemExchange(ExchangeStatus.ACTIVE), POSTS, USERS, Set.of(),
                REQUESTER_ID, AppointmentButton.PROPOSE, false);

        assertEquals(1, result.offeredItems().size());
        assertTrue(result.offeredItems().get(0).editable());
    }

    @Test
    @DisplayName("게시자가 보면 새로 작성한 제품 editable false")
    void of_ownerViewer_isNotEditable() {
        ExchangeResult result = ExchangeResult.of(newItemExchange(ExchangeStatus.ACTIVE), POSTS, USERS, Set.of(),
                OWNER_ID, AppointmentButton.PROPOSE, false);

        assertFalse(result.offeredItems().get(0).editable());
    }

    @Test
    @DisplayName("POST 방식 교환이면 요청자가 봐도 모든 제품 editable false")
    void of_postOffer_isNotEditableForRequester() {
        Exchange exchange = Exchange.of(EXCHANGE_ID, POST_ID, REQUESTER_ID, OWNER_ID, OfferType.POST,
                List.of(OfferedItem.of(ITEM_ID, SOURCE_POST_ID, null, null, null, 0)),
                ExchangeStatus.ACTIVE, null, NOW, NOW);

        ExchangeResult result = ExchangeResult.of(exchange, POSTS, USERS, Set.of(),
                REQUESTER_ID, AppointmentButton.PROPOSE, false);

        assertEquals(1, result.offeredItems().size());
        result.offeredItems().forEach(item -> assertFalse(item.editable()));
    }

    @ParameterizedTest
    @EnumSource(AppointmentButton.class)
    @DisplayName("넘긴 약속 버튼 값이 결과에 그대로 담긴다")
    void of_carriesGivenAppointmentButton(AppointmentButton button) {
        ExchangeResult result = ExchangeResult.of(newItemExchange(ExchangeStatus.ACTIVE), POSTS, USERS, Set.of(),
                OWNER_ID, button, false);

        assertEquals(button, result.appointmentButton());
    }

    // ---- AC-13 ----

    @Test
    @DisplayName("확정 약속으로 잠겼으면 요청자에게도 editable false")
    void of_lockedByConfirmed_isNotEditableForRequester() {
        ExchangeResult result = ExchangeResult.of(newItemExchange(ExchangeStatus.ACTIVE), POSTS, USERS, Set.of(),
                REQUESTER_ID, AppointmentButton.CANCEL, true);

        assertFalse(result.offeredItems().get(0).editable());
    }

    @ParameterizedTest
    @EnumSource(value = ExchangeStatus.class, names = {"COMPLETED", "CLOSED", "LEFT"})
    @DisplayName("끝난(COMPLETED · CLOSED · LEFT) 교환이면 잠금이 없어도 요청자 editable false")
    void of_endedExchange_isNotEditableForRequester(ExchangeStatus status) {
        ExchangeResult result = ExchangeResult.of(newItemExchange(status), POSTS, USERS, Set.of(),
                REQUESTER_ID, AppointmentButton.NONE, false);

        assertFalse(result.offeredItems().get(0).editable());
    }

    private static Exchange newItemExchange(ExchangeStatus status) {
        Instant completedAt = status == ExchangeStatus.COMPLETED ? NOW : null;
        return Exchange.of(EXCHANGE_ID, POST_ID, REQUESTER_ID, OWNER_ID, OfferType.NEW,
                List.of(OfferedItem.of(ITEM_ID, null, new ItemName("치이카와 아크릴 스탠드"), false,
                        new ItemDescription("가챠에서 두 개 나와서 하나 교환해요"), 0)),
                status, completedAt, NOW, NOW);
    }
}
