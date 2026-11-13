package com.ject.user.api.controller.api;

import com.ject.common.web.response.ApiResponse;
import com.ject.user.api.dto.request.DevTokenRequest;
import com.ject.user.api.dto.response.DevTokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "dev 테스트 토큰", description = "local · dev 서버 전용. 소셜 로그인 없이 테스트 회원 토큰 발급")
@RequestMapping("/api/v1/dev")
public interface DevTokenApi {

    @Operation(summary = "테스트 회원 토큰 발급",
            description = "같은 닉네임으로 다시 부르면 같은 회원 토큰을 줍니다. 받은 accessToken을 Swagger 상단 Authorize에 넣으면 됩니다.")
    @SecurityRequirements
    @PostMapping("/tokens")
    ApiResponse<DevTokenResponse> issueToken(@RequestBody(required = false) DevTokenRequest request);
}
