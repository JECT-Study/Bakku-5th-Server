package com.ject.post.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "태그 자동완성")
public record TagSuggestionResponse(
        @Schema(description = "추천 태그 이름 목록. # 제외. 없으면 빈 배열", example = "[\"치이카와\", \"치이카와가챠\"]")
        List<String> tags
) {
}
