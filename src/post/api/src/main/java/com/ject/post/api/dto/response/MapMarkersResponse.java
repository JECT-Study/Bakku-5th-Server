package com.ject.post.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "지도 숫자 핀 목록")
public record MapMarkersResponse(
        @Schema(description = "숫자 핀 목록. 없으면 빈 배열")
        List<MapMarkerResponse> markers
) {
}
