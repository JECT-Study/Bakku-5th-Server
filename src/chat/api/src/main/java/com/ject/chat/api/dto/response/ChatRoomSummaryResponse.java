package com.ject.chat.api.dto.response;

import com.ject.chat.api.dto.enums.RoomBadge;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "채팅 목록 항목")
public record ChatRoomSummaryResponse(
        @Schema(description = "채팅방 ID", example = "5")
        Long chatRoomId,

        @Schema(description = "교환 ID", example = "1")
        Long exchangeId,

        @Schema(description = "상대 요약")
        CounterpartSummaryResponse counterpart,

        @Schema(description = "교환글 요약")
        ChatPostResponse post,

        @Schema(description = "방 배지. 없으면 null", example = "RESERVED")
        RoomBadge badge,

        @Schema(description = "마지막 메시지. 메시지가 없으면 null")
        LastMessageResponse lastMessage,

        @Schema(description = "안 읽은 메시지 수 (실제 값). 99 초과 시 \"99+\" 표시는 FE", example = "3")
        Integer unreadCount,

        @Schema(description = "이 방 알림 켜짐 여부", example = "true")
        boolean notificationEnabled
) {
}
