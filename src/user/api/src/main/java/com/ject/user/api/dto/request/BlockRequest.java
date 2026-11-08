package com.ject.user.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "차단")
public record BlockRequest(
        @Schema(description = "차단할 회원 ID", example = "202")
        @NotNull
        Long userId
) {
}
