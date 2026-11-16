package com.ject.common.web.idempotency;

import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class FakeIdempotencyStore extends IdempotencyStore {

    private static final String IN_PROGRESS = "IN_PROGRESS";

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private final Map<String, String> values = new HashMap<>();

    FakeIdempotencyStore() {
        super(null, null);
    }

    @Override
    public boolean tryLock(String key) {
        return values.putIfAbsent(key, IN_PROGRESS) == null;
    }

    @Override
    public Optional<CachedResponse> findResponse(String key) {
        String value = values.get(key);
        if (value == null || IN_PROGRESS.equals(value)) {
            return Optional.empty();
        }
        return Optional.of(jsonMapper.readValue(value, CachedResponse.class));
    }

    @Override
    public void saveResponse(String key, CachedResponse response, Duration ttl) {
        values.put(key, jsonMapper.writeValueAsString(response));
    }

    @Override
    public void release(String key) {
        values.remove(key);
    }

    boolean contains(String key) {
        return values.containsKey(key);
    }
}
