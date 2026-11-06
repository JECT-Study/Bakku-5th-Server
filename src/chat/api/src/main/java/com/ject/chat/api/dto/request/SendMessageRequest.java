package com.ject.chat.api.dto.request;

import com.ject.chat.api.dto.enums.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "메시지 보내기")
public record SendMessageRequest(
        @Schema(description = "FE가 만든 UUID (36자). 같은 값으로 다시 보내면 새로 저장하지 않고 기존 메시지를 반환", example = "2f1c9a7e-5b1d-4c0e-9e8f-0a1b2c3d4e5f")
        @NotBlank @Size(min = 36, max = 36)
        String clientMessageId,

        @Schema(description = "TEXT / IMAGE / PLACE (2차). SYSTEM · CARD는 보낼 수 없음", example = "TEXT")
        @NotNull
        MessageType type,

        @Schema(description = "TEXT일 때 필수. 1~1,000자, 공백만은 불가", example = "내일 3시 홍대입구역 어떠세요?")
        @Size(max = 1000)
        String text,

        @Schema(description = "IMAGE일 때 필수. 업로드한 이미지 key, 1~5장", example = "[\"chat/tmp/a.jpg\", \"chat/tmp/b.jpg\"]")
        @Size(min = 1, max = 5)
        List<@NotBlank String> imageKeys,

        @Schema(description = "PLACE일 때 필수. 보낼 장소")
        @Valid
        PlaceRequest place
) {
}
