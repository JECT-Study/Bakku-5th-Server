package com.ject.user.infrastructure.persistence.entity;

import com.ject.common.jpa.BaseEntity;
import com.ject.user.domain.domain.DevicePlatform;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 등록 · 갱신은 동시 요청에도 기기 ID당 한 행이 되도록 PushDeviceJpaRepository의 upsert로만 한다.
 */
@Entity
@Table(name = "user_push_device")
public class PushDeviceJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "device_id", nullable = false, updatable = false, length = 100)
    private String deviceId;

    @Column(name = "push_token", nullable = false, length = 512)
    private String pushToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 10)
    private DevicePlatform platform;

    @Column(name = "app_version", length = 20)
    private String appVersion;

    protected PushDeviceJpaEntity() {
    }
}
