package com.ject.user.api.dto.request;

import com.ject.user.api.dto.enums.WithdrawalReason;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원 탈퇴")
public record WithdrawalRequest(
        @Schema(description = "떠나는 이유. 선택", example = "HARD_TO_FIND_PARTNER", nullable = true)
        WithdrawalReason reason
) {
}
