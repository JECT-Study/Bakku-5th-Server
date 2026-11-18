package com.ject.user.application.dto;

/**
 * DV-1 푸시 토큰 등록 · 갱신.
 *
 * @param platform   DevicePlatform 이름 (IOS · ANDROID)
 * @param appVersion 없으면 null
 */
public record RegisterDeviceCommand(String deviceId, String pushToken, String platform, String appVersion) {
}
