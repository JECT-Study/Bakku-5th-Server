package com.ject.exchange.api.controller;

import com.ject.common.response.ApiResponse;
import com.ject.exchange.api.controller.api.ExchangeApi;
import com.ject.exchange.api.dto.enums.AppointmentButton;
import com.ject.exchange.api.dto.enums.ExchangeStatus;
import com.ject.exchange.api.dto.enums.ItemAvailability;
import com.ject.exchange.api.dto.enums.ItemCondition;
import com.ject.exchange.api.dto.enums.OfferType;
import com.ject.exchange.api.dto.enums.PostStatus;
import com.ject.exchange.api.dto.request.CreateExchangeRequest;
import com.ject.exchange.api.dto.request.NewItemRequest;
import com.ject.exchange.api.dto.response.ExchangeResponse;
import com.ject.exchange.api.dto.response.OfferedItemResponse;
import com.ject.exchange.api.dto.response.ParticipantResponse;
import com.ject.exchange.api.dto.response.PostSummaryResponse;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.ject.exchange.api.controller.ExchangeStubData.CHAT_ROOM_ID;
import static com.ject.exchange.api.controller.ExchangeStubData.EXCHANGE_CREATED_AT;
import static com.ject.exchange.api.controller.ExchangeStubData.EXCHANGE_ID;
import static com.ject.exchange.api.controller.ExchangeStubData.OWNER_ID;
import static com.ject.exchange.api.controller.ExchangeStubData.POST_ID;
import static com.ject.exchange.api.controller.ExchangeStubData.REQUESTER_ID;

@RestController
public class ExchangeController implements ExchangeApi {

    @Override
    public ApiResponse<ExchangeResponse> createExchange(CreateExchangeRequest request) {
        return ApiResponse.created(postOfferExchange(EXCHANGE_ID, request.postId()));
    }

    @Override
    public ApiResponse<ExchangeResponse> getExchange(Long exchangeId) {
        return ApiResponse.success(postOfferExchange(exchangeId, POST_ID));
    }

    @Override
    public ApiResponse<ExchangeResponse> updateNewItem(Long exchangeId, NewItemRequest request) {
        OfferedItemResponse newItem = new OfferedItemResponse(
                33L, OfferType.NEW, ItemAvailability.AVAILABLE, true, null,
                "치이카와 아크릴 스탠드", ItemCondition.OPENED, "한 번 꺼내봤어요",
                List.of("https://{cdn}/exchange/1/a.jpg"));
        return ApiResponse.success(new ExchangeResponse(
                exchangeId, ExchangeStatus.ACTIVE, targetPost(POST_ID), requester(), owner(),
                OfferType.NEW, List.of(newItem), CHAT_ROOM_ID, AppointmentButton.PROPOSE, EXCHANGE_CREATED_AT));
    }

    private static ExchangeResponse postOfferExchange(Long exchangeId, Long postId) {
        List<OfferedItemResponse> offeredItems = List.of(
                postItem(31L, ItemAvailability.AVAILABLE,
                        new PostSummaryResponse(21L, "우사기 미니 피규어", "https://{cdn}/post/21/1.jpg", PostStatus.AVAILABLE)),
                postItem(32L, ItemAvailability.RESERVED_ELSEWHERE,
                        new PostSummaryResponse(22L, "하치와레 키링", "https://{cdn}/post/22/1.jpg", PostStatus.RESERVED)),
                postItem(35L, ItemAvailability.AVAILABLE,
                        new PostSummaryResponse(23L, "치이카와 랜덤 뱃지", null, PostStatus.AVAILABLE)));
        return new ExchangeResponse(
                exchangeId, ExchangeStatus.ACTIVE, targetPost(postId), requester(), owner(),
                OfferType.POST, offeredItems, CHAT_ROOM_ID, AppointmentButton.PROPOSE, EXCHANGE_CREATED_AT);
    }

    private static OfferedItemResponse postItem(Long id, ItemAvailability availability, PostSummaryResponse post) {
        return new OfferedItemResponse(id, OfferType.POST, availability, false, post, null, null, null, null);
    }

    private static PostSummaryResponse targetPost(Long postId) {
        return new PostSummaryResponse(postId, "치이카와 우사기 가챠 피규어", "https://{cdn}/post/10/1.jpg", PostStatus.AVAILABLE);
    }

    private static ParticipantResponse requester() {
        return new ParticipantResponse(REQUESTER_ID, "쿠로미덕후", 12);
    }

    private static ParticipantResponse owner() {
        return new ParticipantResponse(OWNER_ID, "하치와레러버", 5);
    }
}
