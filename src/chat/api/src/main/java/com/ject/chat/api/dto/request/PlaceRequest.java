package com.ject.chat.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "보낼 장소")
public record PlaceRequest(
        @Schema(description = "장소 이름 (1~100자)", example = "홍대입구역 9번 출구")
        @NotBlank @Size(max = 100)
        String name,

        @Schema(description = "주소 (최대 255자)", example = "서울 마포구 양화로 160")
        @Size(max = 255)
        String address,

        @Schema(description = "위도", example = "37.5571")
        @NotNull @DecimalMin("-90") @DecimalMax("90")
        Double lat,

        @Schema(description = "경도", example = "126.9245")
        @NotNull @DecimalMin("-180") @DecimalMax("180")
        Double lng
) {
}
