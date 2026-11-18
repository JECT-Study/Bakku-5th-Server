package com.ject.user.domain.domain;

import java.util.Objects;

/**
 * 푸시를 받을 기기. 기기 ID마다 한 행이고, 기기 하나에 회원 하나다 (결정 A12).
 * 로그아웃하면 지워서 그 기기로는 푸시를 보내지 않는다 (G-09-1).
 */
public class PushDevice {

    private final Long memberId;
    private final String deviceId;
    private final String pushToken;
    private final DevicePlatform platform;
    private final String appVersion;

    private PushDevice(Long memberId, String deviceId, String pushToken, DevicePlatform platform,
                       String appVersion) {
        this.memberId = Objects.requireNonNull(memberId);
        this.deviceId = Objects.requireNonNull(deviceId);
        this.pushToken = Objects.requireNonNull(pushToken);
        this.platform = Objects.requireNonNull(platform);
        this.appVersion = appVersion;
    }

    /**
     * @param appVersion 없으면 null
     */
    public static PushDevice register(Long memberId, String deviceId, String pushToken, DevicePlatform platform,
                                      String appVersion) {
        return new PushDevice(memberId, deviceId, pushToken, platform, appVersion);
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getPushToken() {
        return pushToken;
    }

    public DevicePlatform getPlatform() {
        return platform;
    }

    /** 앱 버전. 없으면 null */
    public String getAppVersion() {
        return appVersion;
    }
}
