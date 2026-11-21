package com.ject.user.application;

import com.ject.user.application.port.RefreshTokenRepository;
import com.ject.user.domain.domain.RefreshToken;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

class FakeRefreshTokenRepository implements RefreshTokenRepository {

    private final Map<String, RefreshToken> byDeviceId = new HashMap<>();
    private long sequence = 0;

    @Override
    public Optional<RefreshToken> findByDeviceId(String deviceId) {
        return Optional.ofNullable(byDeviceId.get(deviceId)).map(FakeRefreshTokenRepository::copy);
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return byDeviceId.values().stream()
                .filter(token -> token.getTokenHash().equals(tokenHash))
                .findFirst()
                .map(FakeRefreshTokenRepository::copy);
    }

    @Override
    public Optional<RefreshToken> findByPreviousTokenHash(String previousTokenHash) {
        return byDeviceId.values().stream()
                .filter(token -> Objects.equals(token.getPreviousTokenHash(), previousTokenHash))
                .findFirst()
                .map(FakeRefreshTokenRepository::copy);
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        Long id = token.getId() == null ? ++sequence : token.getId();
        RefreshToken saved = RefreshToken.restore(id, token.getMemberId(), token.getDeviceId(), token.getTokenHash(),
                token.getPreviousTokenHash(), token.getRotatedAt());
        byDeviceId.put(saved.getDeviceId(), saved);
        return copy(saved);
    }

    /**
     * 실제 저장소처럼 저장된 현재 토큰이 회전 전 토큰일 때만 바꾼다.
     */
    @Override
    public boolean saveRotation(RefreshToken rotated) {
        RefreshToken stored = byDeviceId.get(rotated.getDeviceId());
        if (stored == null || !stored.getTokenHash().equals(rotated.getPreviousTokenHash())) {
            return false;
        }
        save(rotated);
        return true;
    }

    @Override
    public void delete(RefreshToken token) {
        byDeviceId.remove(token.getDeviceId());
    }

    @Override
    public void deleteAllByMemberId(Long memberId) {
        byDeviceId.values().removeIf(token -> token.getMemberId().equals(memberId));
    }

    int count() {
        return byDeviceId.size();
    }

    // 저장소 밖에서 바꾼 값이 저장 없이 반영되지 않도록 복사해서 준다
    private static RefreshToken copy(RefreshToken token) {
        return RefreshToken.restore(token.getId(), token.getMemberId(), token.getDeviceId(), token.getTokenHash(),
                token.getPreviousTokenHash(), token.getRotatedAt());
    }
}
