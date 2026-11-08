package com.ject.user.api.controller.api;

import com.ject.common.web.response.ApiResponse;
import com.ject.user.api.dto.request.DeviceRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Device", description = "푸시 토큰 등록 · 갱신")
@RequestMapping("/api/v1/members/me/devices")
public interface DeviceApi {

    @Operation(summary = "DV-1 푸시 토큰 등록 · 갱신",
            description = "로그인 · 가입 직후, 앱 실행 때, 푸시 토큰이 바뀌었을 때 부릅니다. 기기 ID 기준으로 등록 · 갱신하고, 같은 푸시 토큰이 다른 회원에게 있으면 그쪽을 지웁니다. "
                    + "실패해도 사용자에게 알리지 않고 다음 실행 때 다시 부릅니다.")
    @PutMapping
    ApiResponse<Void> registerDevice(@Valid @RequestBody DeviceRequest request);
}
