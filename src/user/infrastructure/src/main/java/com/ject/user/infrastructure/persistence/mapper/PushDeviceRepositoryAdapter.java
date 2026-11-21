package com.ject.user.infrastructure.persistence.mapper;

import com.ject.user.application.port.PushDeviceRepository;
import com.ject.user.domain.domain.PushDevice;
import com.ject.user.infrastructure.persistence.repository.PushDeviceJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class PushDeviceRepositoryAdapter implements PushDeviceRepository {

    private final PushDeviceJpaRepository pushDeviceJpaRepository;

    public PushDeviceRepositoryAdapter(PushDeviceJpaRepository pushDeviceJpaRepository) {
        this.pushDeviceJpaRepository = pushDeviceJpaRepository;
    }

    @Override
    public void save(PushDevice device) {
        pushDeviceJpaRepository.upsert(device.getMemberId(), device.getDeviceId(), device.getPushToken(),
                device.getPlatform().name(), device.getAppVersion());
    }

    @Override
    public void deleteByPushTokenOnOtherDevices(String pushToken, String deviceId) {
        pushDeviceJpaRepository.deleteByPushTokenOnOtherDevices(pushToken, deviceId);
    }

    @Override
    public void deleteByDeviceIdAndMemberId(String deviceId, Long memberId) {
        pushDeviceJpaRepository.deleteByDeviceIdAndMemberId(deviceId, memberId);
    }

    @Override
    public void deleteByDeviceIdOfOtherMembers(String deviceId, Long memberId) {
        pushDeviceJpaRepository.deleteByDeviceIdOfOtherMembers(deviceId, memberId);
    }

    @Override
    public void deleteAllByMemberId(Long memberId) {
        pushDeviceJpaRepository.deleteAllByMemberId(memberId);
    }
}
