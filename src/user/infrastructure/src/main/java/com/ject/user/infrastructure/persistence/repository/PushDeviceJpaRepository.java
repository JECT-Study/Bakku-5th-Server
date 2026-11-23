package com.ject.user.infrastructure.persistence.repository;

import com.ject.user.infrastructure.persistence.entity.PushDeviceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PushDeviceJpaRepository extends JpaRepository<PushDeviceJpaEntity, Long> {

    /**
     * 기기 ID가 같은 행이 있으면 바꾸고 없으면 추가한다. 앱 실행 · 로그인 직후 등록이 동시에 와도 한 행만 남는다.
     */
    @Modifying(flushAutomatically = true)
    @Query(nativeQuery = true, value = """
            INSERT INTO user_push_device (member_id, device_id, push_token, platform, app_version, created_at, updated_at)
            VALUES (:memberId, :deviceId, :pushToken, :platform, :appVersion, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (device_id) DO UPDATE
               SET member_id = EXCLUDED.member_id, push_token = EXCLUDED.push_token, platform = EXCLUDED.platform,
                   app_version = EXCLUDED.app_version, updated_at = CURRENT_TIMESTAMP
            """)
    void upsert(@Param("memberId") Long memberId, @Param("deviceId") String deviceId,
                @Param("pushToken") String pushToken, @Param("platform") String platform,
                @Param("appVersion") String appVersion);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM PushDeviceJpaEntity d WHERE d.pushToken = :pushToken AND d.deviceId <> :deviceId")
    void deleteByPushTokenOnOtherDevices(@Param("pushToken") String pushToken, @Param("deviceId") String deviceId);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM PushDeviceJpaEntity d WHERE d.deviceId = :deviceId AND d.memberId = :memberId")
    void deleteByDeviceIdAndMemberId(@Param("deviceId") String deviceId, @Param("memberId") Long memberId);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM PushDeviceJpaEntity d WHERE d.deviceId = :deviceId AND d.memberId <> :memberId")
    void deleteByDeviceIdOfOtherMembers(@Param("deviceId") String deviceId, @Param("memberId") Long memberId);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM PushDeviceJpaEntity d WHERE d.memberId = :memberId")
    void deleteAllByMemberId(@Param("memberId") Long memberId);

    @Query("SELECT d.pushToken FROM PushDeviceJpaEntity d WHERE d.memberId IN :memberIds")
    List<String> findPushTokensByMemberIds(@Param("memberIds") Collection<Long> memberIds);
}
