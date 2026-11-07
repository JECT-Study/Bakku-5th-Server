package com.ject.post.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "희망 장소")
public record PlaceResponse(
        @Schema(description = "위도", example = "37.5563")
        Double lat,

        @Schema(description = "경도", example = "126.9236")
        Double lng,

        @Schema(description = "장소 이름 또는 구+동+번지", example = "스타벅스 홍대동교점")
        String name,

        @Schema(description = "저장한 값에서 시/도를 뺀 구+동. 구가 없으면 시+동", example = "마포구 동교동")
        String regionName,

        @Schema(description = "만날 곳 이름. 없으면 null", example = "AK플라자 주변")
        String memo
) {
}
