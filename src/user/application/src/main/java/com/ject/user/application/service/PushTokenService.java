package com.ject.user.application.service;

import com.ject.user.application.port.PushDeviceRepository;
import com.ject.user.application.query.PushTokenQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * 알림 BC에 주는 기기 토큰 조회.
 */
@Service
@Transactional(readOnly = true)
public class PushTokenService implements PushTokenQuery {

    private final PushDeviceRepository pushDeviceRepository;

    public PushTokenService(PushDeviceRepository pushDeviceRepository) {
        this.pushDeviceRepository = pushDeviceRepository;
    }

    @Override
    public List<String> getPushTokens(Collection<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return List.of();
        }
        return pushDeviceRepository.findPushTokensByMemberIds(memberIds);
    }
}
