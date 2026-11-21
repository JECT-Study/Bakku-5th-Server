package com.ject.user.application;

import com.ject.user.application.dto.RegisterDeviceCommand;
import com.ject.user.application.service.DeviceService;
import com.ject.user.domain.domain.DevicePlatform;
import com.ject.user.domain.domain.PushDevice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeviceServiceTest {

    private FakePushDeviceRepository pushDeviceRepository;
    private DeviceService deviceService;

    @BeforeEach
    void setUp() {
        pushDeviceRepository = new FakePushDeviceRepository();
        deviceService = new DeviceService(pushDeviceRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"IOS", "ANDROID"})
    @DisplayName("API의 기기 종류 이름을 그대로 받아 등록한다")
    void register(String platform) {
        // when
        deviceService.register(101L, new RegisterDeviceCommand("device-1", "fcm-1", platform, null));

        // then
        PushDevice device = pushDeviceRepository.findByDeviceId("device-1").orElseThrow();
        assertEquals(101L, device.getMemberId());
        assertEquals("fcm-1", device.getPushToken());
        assertEquals(DevicePlatform.valueOf(platform), device.getPlatform());
        assertNull(device.getAppVersion());
    }

    @Test
    @DisplayName("같은 기기면 새 푸시 토큰 · 새 회원으로 바꾼다")
    void register_sameDevice_updates() {
        // given
        deviceService.register(101L, new RegisterDeviceCommand("device-1", "fcm-1", "IOS", "1.0.0"));

        // when
        deviceService.register(202L, new RegisterDeviceCommand("device-1", "fcm-2", "IOS", "1.0.1"));

        // then
        assertEquals(1, pushDeviceRepository.count());
        PushDevice device = pushDeviceRepository.findByDeviceId("device-1").orElseThrow();
        assertEquals(202L, device.getMemberId());
        assertEquals("fcm-2", device.getPushToken());
        assertEquals("1.0.1", device.getAppVersion());
    }

    @Test
    @DisplayName("같은 푸시 토큰이 다른 기기 행에 있으면 그 행을 지운다")
    void register_samePushTokenOnOtherDevice_deletesIt() {
        // given
        deviceService.register(101L, new RegisterDeviceCommand("old-device", "fcm-1", "ANDROID", null));

        // when
        deviceService.register(202L, new RegisterDeviceCommand("new-device", "fcm-1", "ANDROID", null));

        // then
        assertEquals(1, pushDeviceRepository.count());
        assertTrue(pushDeviceRepository.findByDeviceId("old-device").isEmpty());
        assertEquals(202L, pushDeviceRepository.findByDeviceId("new-device").orElseThrow().getMemberId());
    }
}
