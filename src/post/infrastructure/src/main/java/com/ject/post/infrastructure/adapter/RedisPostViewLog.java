package com.ject.post.infrastructure.adapter;

import com.ject.post.application.port.PostViewLog;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class RedisPostViewLog implements PostViewLog {

    private static final String KEY_FORMAT = "post:view:%d:%d:%s";
    private static final Duration TTL = Duration.ofHours(25);

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean markViewed(Long postId, Long viewerId, LocalDate date) {
        String key = KEY_FORMAT.formatted(postId, viewerId, date);
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, "1", TTL));
    }
}
