package com.ject.post.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "희망 장소")
public record PlaceRequest(
        @Schema(description = "위도 (-90 ~ 90)", example = "37.5563")
        @NotNull @DecimalMin("-90") @DecimalMax("90")
        Double lat,

        @Schema(description = "경도 (-180 ~ 180)", example = "126.9236")
        @NotNull @DecimalMin("-180") @DecimalMax("180")
        Double lng,

        @Schema(description = "1~100자. 장소 검색이면 장소 이름, 핀이면 구+동+번지", example = "스타벅스 홍대동교점")
        @NotBlank @Size(max = 100)
        String name,

        @Schema(description = "시/도 + 구 + 동. 구가 없으면 시/도 + 시 + 동. FE가 네이버 결과로 전달", example = "서울특별시 마포구 동교동")
        @NotBlank @Size(max = 100)
        String regionName,

        @Schema(description = "만날 곳 이름 2~30자. 선택", example = "AK플라자 주변")
        @Size(min = 2, max = 30)
        String memo
) {
}
