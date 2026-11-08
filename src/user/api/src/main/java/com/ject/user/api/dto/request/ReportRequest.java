package com.ject.user.api.dto.request;

import com.ject.user.api.dto.enums.ReportReason;
import com.ject.user.api.dto.enums.ReportTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "신고")
public record ReportRequest(
        @Schema(description = "신고 대상 종류. CHAT_MESSAGE는 2차", example = "CHAT_ROOM")
        @NotNull
        ReportTargetType targetType,

        @Schema(description = "신고 대상 ID (교환글 ID · 회원 ID · 첨부 아이템 ID · 채팅방 ID)", example = "5")
        @NotNull
        Long targetId,

        @Schema(description = "신고 사유. ITEM_PROBLEM은 교환을 완료한 상대에게만", example = "EXTERNAL_TRADE")
        @NotNull
        ReportReason reason,

        @Schema(description = "기타 직접 입력 1~300자. OTHER일 때 필수, 아니면 null", nullable = true)
        @Size(max = 300)
        String detail
) {

    @AssertTrue(message = "OTHER는 내용을 입력해야 합니다.")
    @Schema(hidden = true)
    public boolean isDetailPresentForOther() {
        return reason != ReportReason.OTHER || (detail != null && !detail.isBlank());
    }
}
