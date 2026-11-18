package com.ject.user.application.service;

import com.ject.user.application.dto.RegisterDeviceCommand;
import com.ject.user.application.port.PushDeviceRepository;
import com.ject.user.domain.domain.DevicePlatform;
import com.ject.user.domain.domain.PushDevice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 푸시 토큰 등록 · 갱신 (DV-1). 기기 하나에 회원 하나다 (결정 A12).
 */
@Service
public class DeviceService {

    private final PushDeviceRepository pushDeviceRepository;

    public DeviceService(PushDeviceRepository pushDeviceRepository) {
        this.pushDeviceRepository = pushDeviceRepository;
    }

    /**
     * 기기 ID 기준으로 등록 · 갱신한다. 다른 회원이 쓰던 기기면 이 회원 것으로 바뀌고,
     * 같은 푸시 토큰이 다른 기기 행에 있으면 그 행을 지워 다른 계정 알림이 이 기기로 가지 않게 한다.
     */
    @Transactional
    public void register(Long memberId, RegisterDeviceCommand command) {
        PushDevice device = PushDevice.register(memberId, command.deviceId(), command.pushToken(),
                DevicePlatform.valueOf(command.platform()), command.appVersion());
        pushDeviceRepository.deleteByPushTokenOnOtherDevices(device.getPushToken(), device.getDeviceId());
        pushDeviceRepository.save(device);
    }
}
