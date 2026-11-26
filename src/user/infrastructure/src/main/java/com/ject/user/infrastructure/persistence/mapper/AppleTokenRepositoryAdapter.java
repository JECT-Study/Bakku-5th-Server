package com.ject.user.infrastructure.persistence.mapper;

import com.ject.user.application.port.AppleTokenRepository;
import com.ject.user.infrastructure.persistence.entity.AppleTokenJpaEntity;
import com.ject.user.infrastructure.persistence.repository.AppleTokenJpaRepository;
import com.ject.user.infrastructure.security.AppleTokenCipher;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 토큰은 암호화해 저장하고, 읽을 때 복호화한다.
 */
@Repository
public class AppleTokenRepositoryAdapter implements AppleTokenRepository {

    private final AppleTokenJpaRepository appleTokenJpaRepository;
    private final AppleTokenCipher appleTokenCipher;

    public AppleTokenRepositoryAdapter(AppleTokenJpaRepository appleTokenJpaRepository,
                                       AppleTokenCipher appleTokenCipher) {
        this.appleTokenJpaRepository = appleTokenJpaRepository;
        this.appleTokenCipher = appleTokenCipher;
    }

    @Override
    @Transactional
    public void save(String socialIdHash, String refreshToken) {
        String encrypted = appleTokenCipher.encrypt(refreshToken);
        appleTokenJpaRepository.findById(socialIdHash).ifPresentOrElse(
                entity -> entity.changeRefreshToken(encrypted),
                () -> appleTokenJpaRepository.save(new AppleTokenJpaEntity(socialIdHash, encrypted)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> find(String socialIdHash) {
        return appleTokenJpaRepository.findById(socialIdHash)
                .map(entity -> appleTokenCipher.decrypt(entity.getRefreshToken()));
    }

    @Override
    @Transactional
    public void delete(String socialIdHash) {
        appleTokenJpaRepository.deleteById(socialIdHash);
    }
}
