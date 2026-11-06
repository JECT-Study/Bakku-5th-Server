package com.ject.chat.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "PLACE 내용 (2차)")
public record PlaceMessageContent(
        @Schema(description = "장소 이름", example = "홍대입구역 9번 출구")
        String name,

        @Schema(description = "주소. 없으면 null", example = "서울 마포구 양화로 160")
        String address,

        @Schema(description = "위도", example = "37.5571")
        Double lat,

        @Schema(description = "경도", example = "126.9245")
        Double lng
) implements MessageContent {
}
