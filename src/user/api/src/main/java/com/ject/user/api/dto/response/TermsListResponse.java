package com.ject.user.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "약관 목록")
public record TermsListResponse(
        @Schema(description = "약관 목록")
        List<TermsResponse> content
) {
}
