package com.ject.chat.api.controller.api;

import com.ject.chat.api.dto.request.SendMessageRequest;
import com.ject.chat.api.dto.response.CursorPageResponse;
import com.ject.chat.api.dto.response.MessageResponse;
import com.ject.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "Chat Message", description = "메시지 목록 · 메시지 보내기")
@RequestMapping("/api/v1/chat-rooms/{chatRoomId}/messages")
public interface ChatMessageApi {

    @Operation(summary = "CH-3 메시지 목록", description = "cursor로 더 오래된 메시지를 내려받거나(최신순), after로 재연결 후 놓친 메시지만 받습니다. cursor와 after는 함께 쓸 수 없습니다.")
    @GetMapping
    ApiResponse<CursorPageResponse<MessageResponse>> getMessages(
            @PathVariable("chatRoomId") Long chatRoomId,
            @Parameter(description = "이전 응답의 nextCursor")
            @RequestParam(value = "cursor", required = false) String cursor,
            @Parameter(description = "1~100, 기본 30. after와 함께 쓰면 무시")
            @RequestParam(value = "size", defaultValue = "30") @Min(1) @Max(100) Integer size,
            @Parameter(description = "이 메시지 ID 이후만 조회. cursor와 함께 쓸 수 없음")
            @RequestParam(value = "after", required = false) Long after);

    @Operation(summary = "CH-4 메시지 보내기", description = "텍스트 · 사진 · 장소를 보냅니다. 시스템 · 카드 메시지는 서버만 만들 수 있습니다. 보낸 메시지는 WebSocket MESSAGE_CREATED로 상대와 나 모두에게 전달되고, 내 읽음 위치도 이 메시지로 갱신됩니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<MessageResponse> sendMessage(@PathVariable("chatRoomId") Long chatRoomId,
                                             @Valid @RequestBody SendMessageRequest request);
}
