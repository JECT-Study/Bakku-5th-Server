package com.ject.user.api.dto.request;

import com.ject.user.api.dto.enums.DevicePlatform;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "푸시 토큰 등록 · 갱신")
public record DeviceRequest(
        @Schema(description = "앱 설치 단위 ID. FE가 처음 실행 때 UUID를 만들어 기기에 저장", example = "6f1c9a7e-5b1d-4c0e-9e8f-0a1b2c3d4e5f")
        @NotBlank
        String deviceId,

        @Schema(description = "FCM 토큰", example = "fcm-token-value")
        @NotBlank
        String pushToken,

        @Schema(description = "기기 종류", example = "IOS")
        @NotNull
        DevicePlatform platform,

        @Schema(description = "앱 버전. 선택", example = "1.0.0", nullable = true)
        String appVersion
) {
}
