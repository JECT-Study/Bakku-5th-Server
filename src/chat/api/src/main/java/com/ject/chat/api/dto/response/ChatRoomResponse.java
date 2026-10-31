package com.ject.chat.api.dto.response;

import com.ject.chat.api.dto.enums.InputDisabledReason;
import com.ject.chat.api.dto.enums.LeaveImpact;
import com.ject.chat.api.dto.enums.RoomStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "채팅방 상세")
public record ChatRoomResponse(
        @Schema(description = "채팅방 ID", example = "5")
        Long chatRoomId,

        @Schema(description = "교환 ID. 상단 교환 카드는 Exchange API E-2, A-3으로 조회", example = "1")
        Long exchangeId,

        @Schema(description = "채팅방 상태", example = "OPEN")
        RoomStatus status,

        @Schema(description = "입력창 대신 보여줄 안내. 입력 가능하면 null")
        InputDisabledReason inputDisabledReason,

        @Schema(description = "상대 정보")
        CounterpartResponse counterpart,

        @Schema(description = "이 방 알림 켜짐 여부", example = "true")
        boolean notificationEnabled,

        @Schema(description = "나가기 확인 팝업 종류", example = "NONE")
        LeaveImpact leaveImpact,

        @Schema(description = "내가 마지막으로 읽은 메시지 ID. 없으면 null", example = "117")
        Long myLastReadMessageId
) {
}
