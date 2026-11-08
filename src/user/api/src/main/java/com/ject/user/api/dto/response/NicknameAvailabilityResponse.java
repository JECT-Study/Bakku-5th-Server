package com.ject.user.api.dto.response;

import com.ject.user.api.dto.enums.NicknameUnavailableReason;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "닉네임 사용 가능 여부")
public record NicknameAvailabilityResponse(
        @Schema(description = "사용 가능 여부", example = "false")
        boolean available,

        @Schema(description = "사용할 수 없는 이유. 사용 가능하면 null", example = "DUPLICATE", nullable = true)
        NicknameUnavailableReason reason,

        @Schema(description = "금지어 포함 여부. 경고만, 막지 않음. 2차라 1차는 항상 false", example = "false")
        boolean bannedWordDetected
) {
}
