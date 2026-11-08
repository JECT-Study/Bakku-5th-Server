package com.ject.user.api.controller.api;

import com.ject.common.web.response.ApiResponse;
import com.ject.user.api.dto.response.TermsListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Terms", description = "약관 목록")
@RequestMapping("/api/v1/terms")
public interface TermsApi {

    @Operation(summary = "TM-1 약관 목록",
            description = "가입 약관 동의 화면과 설정 > 약관 및 정책에 씁니다. 가입 때 저장할 약관 버전을 여기서 받습니다. 본문은 url을 웹뷰로 엽니다. 인증 없이 호출합니다.")
    @SecurityRequirements
    @GetMapping
    ApiResponse<TermsListResponse> getTerms();
}
