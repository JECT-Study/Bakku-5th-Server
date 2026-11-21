package com.ject.user.application;

import com.ject.user.application.port.RefreshTokenRepository;
import com.ject.user.domain.domain.RefreshToken;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class FakeRefreshTokenRepository implements RefreshTokenRepository {

    private final Map<String, RefreshToken> byDeviceId = new HashMap<>();
    private long sequence = 0;

    @Override
    public Optional<RefreshToken> findByDeviceId(String deviceId) {
        return Optional.ofNullable(byDeviceId.get(deviceId));
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        Long id = token.getId() == null ? ++sequence : token.getId();
        RefreshToken saved = RefreshToken.restore(id, token.getMemberId(), token.getDeviceId(), token.getTokenHash(),
                token.getPreviousTokenHash(), token.getRotatedAt());
        byDeviceId.put(saved.getDeviceId(), saved);
        return saved;
    }

    int count() {
        return byDeviceId.size();
    }
}
