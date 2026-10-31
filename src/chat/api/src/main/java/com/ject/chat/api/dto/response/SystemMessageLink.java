package com.ject.chat.api.dto.response;

import com.ject.chat.api.dto.enums.SystemLinkType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "시스템 메시지 [보기] 버튼 대상")
public record SystemMessageLink(
        @Schema(description = "대상 종류", example = "POST")
        SystemLinkType type,

        @Schema(description = "대상 ID", example = "21")
        Long id
) {
}
