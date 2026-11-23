package com.ject.post.infrastructure.adapter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisPostViewLogTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 23);

    @Test
    @DisplayName("오늘 처음 보면 true, 같은 날 다시 보면 false")
    void markViewed_firstTimeOnly() {
        FakeRedisTemplate redis = new FakeRedisTemplate(null);
        RedisPostViewLog viewLog = new RedisPostViewLog(redis);

        assertTrue(viewLog.markViewed(10L, 3L, TODAY));
        assertFalse(viewLog.markViewed(10L, 3L, TODAY));
        assertEquals(Set.of("post:view:10:3:2026-11-23"), redis.keys);
    }

    @Test
    @DisplayName("Redis가 실패하면 예외를 밖으로 내지 않고 안 센 것(false)으로 처리한다")
    void markViewed_redisFails_returnsFalse() {
        RedisPostViewLog viewLog = new RedisPostViewLog(new FakeRedisTemplate(
                new RedisSystemException("Error in execution",
                        new IllegalStateException("NOPERM No permissions to access a key"))));

        assertFalse(viewLog.markViewed(10L, 3L, TODAY));
    }

    private static class FakeRedisTemplate extends StringRedisTemplate {

        private final RuntimeException failure;
        private final Set<String> keys = new HashSet<>();

        FakeRedisTemplate(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        @SuppressWarnings("unchecked")
        public ValueOperations<String, String> opsForValue() {
            return (ValueOperations<String, String>) Proxy.newProxyInstance(
                    ValueOperations.class.getClassLoader(),
                    new Class<?>[]{ValueOperations.class},
                    (proxy, method, args) -> {
                        if (!method.getName().equals("setIfAbsent")) {
                            throw new UnsupportedOperationException(method.getName());
                        }
                        if (failure != null) {
                            throw failure;
                        }
                        return keys.add((String) args[0]);
                    });
        }
    }
}
