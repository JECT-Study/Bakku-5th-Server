package com.ject.user.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "탈퇴 전 안내 정보")
public record WithdrawalNoticeResponse(
        @Schema(description = "진행 중인 교환(확정 약속) 수. 0이면 안내 문구 없음", example = "1")
        int inProgressExchangeCount
) {
}
