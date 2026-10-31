package com.ject.chat.api.controller.api;

import com.ject.chat.api.dto.request.ReadMessagesRequest;
import com.ject.chat.api.dto.request.UpdateRoomNotificationRequest;
import com.ject.chat.api.dto.response.ChatRoomResponse;
import com.ject.chat.api.dto.response.ChatRoomSummaryResponse;
import com.ject.chat.api.dto.response.CursorPageResponse;
import com.ject.chat.api.dto.response.LeaveChatRoomResponse;
import com.ject.chat.api.dto.response.ReadMessagesResponse;
import com.ject.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Chat Room", description = "채팅 목록 · 채팅방 상세 · 읽음 처리 · 방 알림 · 나가기")
@RequestMapping("/api/v1")
public interface ChatRoomApi {

    @Operation(summary = "CH-1 채팅 목록", description = "최근 메시지 시각 내림차순입니다. 내가 나간 방과 차단한 상대와의 방은 제외하고, 상대가 나간 방 · 교환이 끝난 방은 그대로 남깁니다.")
    @GetMapping("/members/me/chat-rooms")
    ApiResponse<CursorPageResponse<ChatRoomSummaryResponse>> getChatRooms(
            @Parameter(description = "이전 응답의 nextCursor. 첫 요청은 생략")
            @RequestParam(value = "cursor", required = false) String cursor,
            @Parameter(description = "1~50, 기본 20")
            @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(50) Integer size);

    @Operation(summary = "CH-2 채팅방 상세", description = "채팅방 진입 시 입력창 상태, 상대 정보, 나가기 팝업 종류를 조회합니다.")
    @GetMapping("/chat-rooms/{chatRoomId}")
    ApiResponse<ChatRoomResponse> getChatRoom(@PathVariable("chatRoomId") Long chatRoomId);

    @Operation(summary = "CH-5 읽음 처리", description = "채팅방에 들어가거나, 방에 있는 동안 새 메시지를 받으면 호출합니다. 안 읽은 수 계산 기준만 바꾸며 상대에게 읽음 표시는 보여주지 않습니다. 저장된 값보다 작은 ID가 오면 무시합니다.")
    @PostMapping("/chat-rooms/{chatRoomId}/read")
    ApiResponse<ReadMessagesResponse> readMessages(@PathVariable("chatRoomId") Long chatRoomId,
                                                   @Valid @RequestBody ReadMessagesRequest request);

    @Operation(summary = "CH-6 이 방 알림 끄기 · 켜기", description = "이 방의 채팅 알림만 끄고 켭니다. 약속 · 교환 확인 같은 시스템 알림도 끄는지는 미결입니다.")
    @PutMapping("/chat-rooms/{chatRoomId}/notification")
    ApiResponse<ChatRoomResponse> updateNotification(@PathVariable("chatRoomId") Long chatRoomId,
                                                     @Valid @RequestBody UpdateRoomNotificationRequest request);

    @Operation(summary = "CH-7 채팅방 나가기", description = "나간 방에는 다시 들어올 수 없습니다. FE는 CH-2의 leaveImpact로 확인 팝업을 고른 뒤 호출합니다.")
    @PostMapping("/chat-rooms/{chatRoomId}/leave")
    ApiResponse<LeaveChatRoomResponse> leaveChatRoom(@PathVariable("chatRoomId") Long chatRoomId);
}
