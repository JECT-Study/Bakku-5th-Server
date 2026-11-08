package com.ject.user.api.controller.api;

import com.ject.common.web.response.ApiResponse;
import com.ject.user.api.dto.enums.DevicePlatform;
import com.ject.user.api.dto.response.AppVersionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "App", description = "앱 최소 지원 버전")
@RequestMapping("/api/v1/app")
public interface AppVersionApi {

    @Operation(summary = "AP-1 앱 최소 지원 버전",
            description = "앱 실행 때(스플래시) 부릅니다. 앱 버전이 minimumVersion보다 낮으면 FE가 강제 업데이트 팝업을 띄웁니다. 값은 서버 설정으로 관리해 배포 없이 바꿉니다. 인증 없이 호출합니다.")
    @SecurityRequirements
    @GetMapping("/version")
    ApiResponse<AppVersionResponse> getVersion(
            @Parameter(description = "IOS / ANDROID", example = "IOS")
            @RequestParam("platform") DevicePlatform platform);
}
