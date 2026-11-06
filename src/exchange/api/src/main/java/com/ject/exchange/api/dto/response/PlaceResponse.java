package com.ject.exchange.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "약속 장소")
public record PlaceResponse(
        @Schema(description = "장소 이름", example = "홍대입구역 3번 출구 앞 편의점")
        String name,

        @Schema(description = "주소. 없으면 null", example = "서울 마포구 양화로 160")
        String address,

        @Schema(description = "위도", example = "37.5571")
        Double lat,

        @Schema(description = "경도", example = "126.9245")
        Double lng
) {
}
