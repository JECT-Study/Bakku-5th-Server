package com.ject.user.application.port;

import com.ject.user.domain.domain.PushDevice;

import java.util.Collection;
import java.util.List;

/**
 * 푸시 기기 저장소 출력 포트. 기기 ID마다 한 행이다 (결정 A12).
 */
public interface PushDeviceRepository {

    /**
     * 기기 ID가 같은 행이 있으면 그 행을 바꾸고, 없으면 추가한다.
     */
    void save(PushDevice device);

    /**
     * 같은 푸시 토큰을 가진 다른 기기 행을 지운다.
     */
    void deleteByPushTokenOnOtherDevices(String pushToken, String deviceId);

    void deleteByDeviceIdAndMemberId(String deviceId, Long memberId);

    /**
     * 다른 회원이 이 기기로 로그인했을 때 이전 회원의 행을 지운다.
     */
    void deleteByDeviceIdOfOtherMembers(String deviceId, Long memberId);

    /**
     * 회원의 모든 푸시 기기를 지운다. 탈퇴한 회원의 기기로 알림이 가지 않게 한다.
     */
    void deleteAllByMemberId(Long memberId);

    List<String> findPushTokensByMemberIds(Collection<Long> memberIds);

    void deleteByPushTokens(Collection<String> pushTokens);
}
