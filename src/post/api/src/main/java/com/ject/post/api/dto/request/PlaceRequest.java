package com.ject.post.api.dto.request;

import com.ject.post.application.dto.command.CreatePostCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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

        @Schema(description = "시/도 + 구 + 동. 구가 없으면 시/도 + 시 + 동. FE가 네이버 결과로 전달. "
                + "시/도 정식 이름(예: 서울특별시, 경기도)으로 시작하고 공백으로 나눈 2~4단어",
                example = "서울특별시 마포구 동교동")
        @NotBlank @Size(max = 100)
        @Pattern(regexp = REGION_NAME, message = "시/도로 시작하는 지역만 넣을 수 있습니다.")
        String regionName,

        @Schema(description = "만날 곳 이름 2~30자. 선택", example = "AK플라자 주변")
        @Size(min = 2, max = 30)
        String memo
) {

    private static final String REGION_NAME = "^(서울특별시|부산광역시|대구광역시|인천광역시|광주광역시|대전광역시|울산광역시"
            + "|세종특별자치시|경기도|강원특별자치도|강원도|충청북도|충청남도|전북특별자치도|전라북도|전라남도"
            + "|경상북도|경상남도|제주특별자치도)(\\s+\\S+){1,3}$";

    public CreatePostCommand.PlaceCommand toCommand() {
        return new CreatePostCommand.PlaceCommand(lat, lng, name, regionName, memo);
    }
}
