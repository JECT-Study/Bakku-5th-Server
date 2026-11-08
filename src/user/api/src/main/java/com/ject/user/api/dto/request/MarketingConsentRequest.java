package com.ject.user.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "마케팅 수신 동의 변경")
public record MarketingConsentRequest(
        @Schema(description = "true 동의 / false 철회", example = "false")
        @NotNull
        Boolean agreed
) {
}
