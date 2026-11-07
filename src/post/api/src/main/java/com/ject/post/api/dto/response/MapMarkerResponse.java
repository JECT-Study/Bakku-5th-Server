package com.ject.post.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "지도 숫자 핀")
public record MapMarkerResponse(
        @Schema(description = "핀 위도. 묶인 글들의 중심", example = "37.5563")
        Double lat,

        @Schema(description = "핀 경도. 묶인 글들의 중심", example = "126.9236")
        Double lng,

        @Schema(description = "이 핀에 묶인 글 수", example = "12")
        int count
) {
}
