package com.ject.user.infrastructure.persistence.entity;

import com.ject.common.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Apple 리프레시 토큰. refreshToken은 암호문(AppleTokenCipher)이다.
 */
@Entity
@Table(name = "user_apple_token")
public class AppleTokenJpaEntity extends BaseEntity {

    @Id
    @Column(name = "social_id_hash", length = 64)
    private String socialIdHash;

    @Column(name = "refresh_token", nullable = false, columnDefinition = "TEXT")
    private String refreshToken;

    protected AppleTokenJpaEntity() {
    }

    public AppleTokenJpaEntity(String socialIdHash, String refreshToken) {
        this.socialIdHash = socialIdHash;
        this.refreshToken = refreshToken;
    }

    public void changeRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
