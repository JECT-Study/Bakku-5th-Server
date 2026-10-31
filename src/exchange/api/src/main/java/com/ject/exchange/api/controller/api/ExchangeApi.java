package com.ject.exchange.api.controller.api;

import com.ject.common.response.ApiResponse;
import com.ject.exchange.api.dto.request.CreateExchangeRequest;
import com.ject.exchange.api.dto.request.NewItemRequest;
import com.ject.exchange.api.dto.response.ExchangeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "Exchange", description = "교환 요청 · 상세 · 새로 작성한 아이템 수정")
@RequestMapping("/api/v1/exchanges")
public interface ExchangeApi {

    @Operation(summary = "E-1 교환 요청", description = "교환 요청 저장과 채팅방 생성을 한 번에 처리합니다. 응답의 chatRoomId로 바로 채팅방에 들어갑니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<ExchangeResponse> createExchange(@Valid @RequestBody CreateExchangeRequest request);

    @Operation(summary = "E-2 교환 상세", description = "채팅방 상단 고정 카드, 첫 메시지 아이템 카드, 아이템 상세, 입력창 위 버튼 상태에 사용합니다.")
    @GetMapping("/{exchangeId}")
    ApiResponse<ExchangeResponse> getExchange(@PathVariable("exchangeId") Long exchangeId);

    @Operation(summary = "E-3 새로 작성한 아이템 수정", description = "요청자가 새로 작성한 아이템을 수정합니다. 확정 약속이 있으면 수정할 수 없습니다.")
    @PutMapping("/{exchangeId}/new-item")
    ApiResponse<ExchangeResponse> updateNewItem(@PathVariable("exchangeId") Long exchangeId,
                                                @Valid @RequestBody NewItemRequest request);
}
