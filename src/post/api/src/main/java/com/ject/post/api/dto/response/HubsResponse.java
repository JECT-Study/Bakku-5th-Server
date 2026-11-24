package com.ject.post.api.dto.response;

import com.ject.post.api.dto.enums.PostHub;
import com.ject.post.application.dto.HubResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "거점 목록")
public record HubsResponse(
        @Schema(description = "거점 선택 시트 · 지도 거점 칩에 보여줄 순서")
        List<HubResponse> hubs
) {

    public static HubsResponse from(List<HubResult> results) {
        return new HubsResponse(results.stream().map(HubResponse::from).toList());
    }

    @Schema(description = "거점")
    public record HubResponse(
            @Schema(description = "거점 코드. 홈 목록 hub 파라미터에 그대로 보냄", example = "HONGDAE")
            PostHub code,

            @Schema(description = "화면 이름", example = "홍대")
            String name,

            @Schema(description = "중심 좌표. 거점 칩을 누르면 지도를 이 위치로 옮김")
            CoordinateResponse center,

            @Schema(description = "거점 범위 다각형 꼭짓점(순서대로). 핀이 이 안이면 지도 하단에 거점 이름 표시")
            List<CoordinateResponse> polygon
    ) {

        static HubResponse from(HubResult result) {
            PostHub code = PostHub.valueOf(result.code());
            return new HubResponse(code, code.getDisplayName(), CoordinateResponse.from(result.center()),
                    result.polygon().stream().map(CoordinateResponse::from).toList());
        }
    }

    @Schema(description = "좌표")
    public record CoordinateResponse(
            @Schema(description = "위도", example = "37.55622")
            double lat,

            @Schema(description = "경도", example = "126.92338")
            double lng
    ) {

        static CoordinateResponse from(HubResult.Coordinate coordinate) {
            return new CoordinateResponse(coordinate.lat(), coordinate.lng());
        }
    }
}
