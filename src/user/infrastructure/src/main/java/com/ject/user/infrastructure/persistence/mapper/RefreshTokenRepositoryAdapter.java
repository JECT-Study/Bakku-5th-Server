package com.ject.user.infrastructure.persistence.mapper;

import com.ject.user.application.port.RefreshTokenRepository;
import com.ject.user.domain.domain.RefreshToken;
import com.ject.user.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import com.ject.user.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository refreshTokenJpaRepository;

    public RefreshTokenRepositoryAdapter(RefreshTokenJpaRepository refreshTokenJpaRepository) {
        this.refreshTokenJpaRepository = refreshTokenJpaRepository;
    }

    @Override
    public Optional<RefreshToken> findByDeviceId(String deviceId) {
        return refreshTokenJpaRepository.findByDeviceId(deviceId).map(RefreshTokenRepositoryAdapter::toDomain);
    }

    /**
     * 새 토큰은 추가하고, 저장된 토큰은 불러온 엔티티의 값을 바꾼다.
     */
    @Override
    public RefreshToken save(RefreshToken token) {
        RefreshTokenJpaEntity entity = token.getId() == null
                ? new RefreshTokenJpaEntity(token.getMemberId(), token.getDeviceId(), token.getTokenHash(),
                        token.getPreviousTokenHash(), token.getRotatedAt())
                : refreshTokenJpaRepository.findById(token.getId()).orElseThrow();
        entity.update(token.getMemberId(), token.getTokenHash(), token.getPreviousTokenHash(), token.getRotatedAt());
        return toDomain(refreshTokenJpaRepository.save(entity));
    }

    private static RefreshToken toDomain(RefreshTokenJpaEntity entity) {
        return RefreshToken.restore(entity.getId(), entity.getMemberId(), entity.getDeviceId(), entity.getTokenHash(),
                entity.getPreviousTokenHash(), entity.getRotatedAt());
    }
}
