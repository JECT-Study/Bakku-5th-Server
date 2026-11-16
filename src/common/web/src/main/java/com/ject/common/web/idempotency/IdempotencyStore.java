package com.ject.common.web.idempotency;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class IdempotencyStore {

    private static final String KEY_PREFIX = "idempotency:";
    private static final String IN_PROGRESS = "IN_PROGRESS";
    private static final Duration LOCK_TTL = Duration.ofSeconds(30);

    private final StringRedisTemplate redisTemplate;
    private final JsonMapper jsonMapper;

    public boolean tryLock(String key){
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + key, IN_PROGRESS, LOCK_TTL);
        return Boolean.TRUE.equals(locked);
    }
    public Optional<CachedResponse> findResponse(String key){
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + key);
        if (value == null || IN_PROGRESS.equals(value)) {
            return Optional.empty();
        }
        return Optional.of(jsonMapper.readValue(value, CachedResponse.class));
    }

    public void saveResponse(String key, CachedResponse response, Duration ttl){
        redisTemplate.opsForValue().set(KEY_PREFIX + key, jsonMapper.writeValueAsString(response), ttl);
    }

    public void release(String key){
        redisTemplate.delete(KEY_PREFIX + key);
    }
}
