package com.ject.user.application;

import com.ject.user.application.service.PushTokenService;
import com.ject.user.domain.domain.DevicePlatform;
import com.ject.user.domain.domain.PushDevice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PushTokenServiceTest {

    private FakePushDeviceRepository pushDeviceRepository;
    private PushTokenService pushTokenService;

    @BeforeEach
    void setUp() {
        pushDeviceRepository = new FakePushDeviceRepository();
        pushTokenService = new PushTokenService(pushDeviceRepository);
    }

    @Test
    @DisplayName("요청한 회원들의 기기 토큰을 모두 준다 (기기가 여러 대면 전부)")
    void getPushTokens() {
        // given
        register(101L, "device-1", "fcm-1");
        register(101L, "device-2", "fcm-2");
        register(202L, "device-3", "fcm-3");
        register(303L, "device-4", "fcm-4");

        // when
        List<String> tokens = pushTokenService.getPushTokens(List.of(101L, 202L));

        // then
        assertEquals(3, tokens.size());
        assertTrue(tokens.containsAll(List.of("fcm-1", "fcm-2", "fcm-3")));
    }

    @Test
    @DisplayName("로그아웃해서 기기 행이 지워진 회원은 토큰이 없다")
    void loggedOutDeviceIsExcluded() {
        // given
        register(101L, "device-1", "fcm-1");
        pushDeviceRepository.deleteByDeviceIdAndMemberId("device-1", 101L);

        // when
        List<String> tokens = pushTokenService.getPushTokens(List.of(101L));

        // then
        assertTrue(tokens.isEmpty());
    }

    private void register(Long memberId, String deviceId, String pushToken) {
        pushDeviceRepository.save(PushDevice.register(memberId, deviceId, pushToken, DevicePlatform.IOS, null));
    }
}
