package com.ject.chat.api.dto.response;

import com.ject.chat.api.dto.enums.SystemMessageCode;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "SYSTEM 내용")
public record SystemMessageContent(
        @Schema(description = "시스템 메시지 코드", example = "ATTACHED_POST_UPDATED")
        SystemMessageCode code,

        @Schema(description = "서버가 만든 문구", example = "상대가 첨부한 글을 수정했어요")
        String text,

        @Schema(description = "[보기] 버튼 대상. 없으면 null")
        SystemMessageLink link
) implements MessageContent {
}
