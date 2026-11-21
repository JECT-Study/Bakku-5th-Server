package com.ject.exchange.api.controller;

import com.ject.common.core.cqrs.command.CommandBus;
import com.ject.common.core.cqrs.query.QueryBus;
import com.ject.common.web.response.ApiResponse;
import com.ject.exchange.api.controller.api.ExchangeApi;
import com.ject.exchange.api.dto.request.CreateExchangeRequest;
import com.ject.exchange.api.dto.request.NewItemRequest;
import com.ject.exchange.api.dto.response.ExchangeResponse;
import com.ject.exchange.application.dto.command.UpdateNewItemCommand;
import com.ject.exchange.application.dto.dto.ExchangeResult;
import com.ject.exchange.application.dto.query.GetExchangeQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ExchangeController implements ExchangeApi {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    @Override
    public ApiResponse<ExchangeResponse> createExchange(Long memberId, CreateExchangeRequest request) {
        Long exchangeId = commandBus.send(request.toCommand(memberId));
        ExchangeResult result = queryBus.send(GetExchangeQuery.builder()
                .exchangeId(exchangeId)
                .viewerId(memberId)
                .build());
        return ApiResponse.created(ExchangeResponse.from(result));
    }

    @Override
    public ApiResponse<ExchangeResponse> getExchange(Long memberId, Long exchangeId) {
        ExchangeResult result = queryBus.send(GetExchangeQuery.builder()
                .exchangeId(exchangeId)
                .viewerId(memberId)
                .build());
        return ApiResponse.success(ExchangeResponse.from(result));
    }

    @Override
    public ApiResponse<ExchangeResponse> updateNewItem(Long memberId, Long exchangeId, NewItemRequest request) {
        Long id = commandBus.send(new UpdateNewItemCommand(exchangeId, memberId, request.toNewItem()));
        ExchangeResult result = queryBus.send(GetExchangeQuery.builder().exchangeId(id).viewerId(memberId).build());
        return ApiResponse.success(ExchangeResponse.from(result));
    }
}
