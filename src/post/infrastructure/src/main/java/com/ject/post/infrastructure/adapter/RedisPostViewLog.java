package com.ject.post.infrastructure.adapter;

import com.ject.post.application.port.PostViewLog;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class RedisPostViewLog implements PostViewLog {

    private static final Logger log = LoggerFactory.getLogger(RedisPostViewLog.class);
    private static final String KEY_FORMAT = "post:view:%d:%d:%s";
    private static final Duration TTL = Duration.ofHours(25);

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean markViewed(Long postId, Long viewerId, LocalDate date) {
        String key = KEY_FORMAT.formatted(postId, viewerId, date);
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, "1", TTL));
        } catch (DataAccessException e) {
            log.warn("조회 수 기록 실패 postId={} viewerId={}: {}",
                    postId, viewerId, e.getMostSpecificCause().getMessage());
            return false;
        }
    }
}
