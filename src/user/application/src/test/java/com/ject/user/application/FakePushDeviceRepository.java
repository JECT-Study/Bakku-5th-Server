package com.ject.user.application;

import com.ject.user.application.port.PushDeviceRepository;
import com.ject.user.domain.domain.PushDevice;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class FakePushDeviceRepository implements PushDeviceRepository {

    private final Map<String, PushDevice> byDeviceId = new HashMap<>();

    @Override
    public void save(PushDevice device) {
        byDeviceId.put(device.getDeviceId(), device);
    }

    @Override
    public void deleteByPushTokenOnOtherDevices(String pushToken, String deviceId) {
        byDeviceId.values().removeIf(device -> device.getPushToken().equals(pushToken)
                && !device.getDeviceId().equals(deviceId));
    }

    @Override
    public void deleteByDeviceIdAndMemberId(String deviceId, Long memberId) {
        byDeviceId.values().removeIf(device -> device.getDeviceId().equals(deviceId)
                && device.getMemberId().equals(memberId));
    }

    @Override
    public void deleteByDeviceIdOfOtherMembers(String deviceId, Long memberId) {
        byDeviceId.values().removeIf(device -> device.getDeviceId().equals(deviceId)
                && !device.getMemberId().equals(memberId));
    }

    @Override
    public void deleteAllByMemberId(Long memberId) {
        byDeviceId.values().removeIf(device -> device.getMemberId().equals(memberId));
    }

    @Override
    public List<String> findPushTokensByMemberIds(Collection<Long> memberIds) {
        return byDeviceId.values().stream()
                .filter(device -> memberIds.contains(device.getMemberId()))
                .map(PushDevice::getPushToken)
                .toList();
    }

    Optional<PushDevice> findByDeviceId(String deviceId) {
        return Optional.ofNullable(byDeviceId.get(deviceId));
    }

    int count() {
        return byDeviceId.size();
    }
}
