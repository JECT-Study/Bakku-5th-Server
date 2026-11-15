package com.ject.user.infrastructure.persistence.entity;

import com.ject.common.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "user_refresh_token")
public class RefreshTokenJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "device_id", nullable = false, updatable = false, length = 100)
    private String deviceId;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "previous_token_hash", length = 64)
    private String previousTokenHash;

    @Column(name = "rotated_at", nullable = false)
    private Instant rotatedAt;

    protected RefreshTokenJpaEntity() {
    }

    public RefreshTokenJpaEntity(Long memberId, String deviceId, String tokenHash, String previousTokenHash,
                                 Instant rotatedAt) {
        this.deviceId = deviceId;
        update(memberId, tokenHash, previousTokenHash, rotatedAt);
    }

    public void update(Long memberId, String tokenHash, String previousTokenHash, Instant rotatedAt) {
        this.memberId = memberId;
        this.tokenHash = tokenHash;
        this.previousTokenHash = previousTokenHash;
        this.rotatedAt = rotatedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public String getPreviousTokenHash() {
        return previousTokenHash;
    }

    public Instant getRotatedAt() {
        return rotatedAt;
    }
}
