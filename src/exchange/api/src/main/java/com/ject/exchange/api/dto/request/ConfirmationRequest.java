package com.ject.exchange.api.dto.request;

import com.ject.exchange.api.dto.enums.ConfirmationAnswer;
import com.ject.exchange.api.dto.enums.NotMetReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "교환 확인 응답")
public record ConfirmationRequest(
        @Schema(description = "교환 확인 응답", example = "DONE")
        @NotNull
        ConfirmationAnswer answer,

        @Schema(description = "NOT_MET일 때 필수. DONE이면 null")
        NotMetReason notMetReason,

        @Schema(description = "notMetReason이 OTHER일 때 직접 입력 (0~100자). OTHER가 아니면 null")
        @Size(max = 100)
        String notMetDetail
) {
}
