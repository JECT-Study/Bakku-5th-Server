package com.ject.post.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "희망 장소")
public record PlaceResponse(
        @Schema(description = "위도", example = "37.5563")
        Double lat,

        @Schema(description = "경도", example = "126.9236")
        Double lng,

        @Schema(description = "장소 검색으로 고른 장소 이름", example = "스타벅스 홍대동교점")
        String name,

        @Schema(description = "저장한 원본 (시/도 + 구 + 동). 수정할 때 그대로 다시 보낸다", example = "서울특별시 마포구 동교동")
        String regionName,

        @Schema(description = "상세 주소. 없으면 null", example = "AK플라자 주변")
        String memo
) {
}
