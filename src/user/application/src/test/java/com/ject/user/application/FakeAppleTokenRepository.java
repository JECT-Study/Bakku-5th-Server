package com.ject.user.application;

import com.ject.user.application.port.AppleTokenRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class FakeAppleTokenRepository implements AppleTokenRepository {

    private final Map<String, String> store = new HashMap<>();

    @Override
    public void save(String socialIdHash, String refreshToken) {
        store.put(socialIdHash, refreshToken);
    }

    @Override
    public Optional<String> find(String socialIdHash) {
        return Optional.ofNullable(store.get(socialIdHash));
    }

    @Override
    public void delete(String socialIdHash) {
        store.remove(socialIdHash);
    }
}
