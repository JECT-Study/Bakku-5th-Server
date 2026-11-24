package com.ject.user.application.service;

import com.ject.user.application.command.PushTokenCommand;
import com.ject.user.application.port.PushDeviceRepository;
import com.ject.user.application.query.PushTokenQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * 알림 BC에 주는 기기 토큰 조회 · 정리.
 */
@Service
@Transactional(readOnly = true)
public class PushTokenService implements PushTokenQuery, PushTokenCommand {

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

    /**
     * 알림 저장 트랜잭션 안에서 불려도 실패가 알림 저장까지 되돌리지 않게 따로 커밋한다.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deletePushTokens(Collection<String> pushTokens) {
        if (pushTokens.isEmpty()) {
            return;
        }
        pushDeviceRepository.deleteByPushTokens(pushTokens);
    }
}
