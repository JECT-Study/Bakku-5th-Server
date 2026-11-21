package com.ject.user.api.ratelimit;

import com.ject.common.core.time.ServiceTime;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * IP별 요청 수를 서버 메모리에서 센다 (결정 C7). 시간 창이 바뀌면 전부 비워서 메모리가 쌓이지 않는다.
 * 서버가 여러 대면 대수만큼 느슨해지고 창 경계에서는 최대 두 배까지 통과하지만, 목록 긁기 방지 목적이라 허용한다.
 */
public class IpRateLimiter {

    private final int limit;
    private final long windowMillis;
    private final ServiceTime serviceTime;
    private final Map<String, AtomicInteger> counts = new ConcurrentHashMap<>();
    private long currentWindow = -1;

    public IpRateLimiter(int limit, Duration window, ServiceTime serviceTime) {
        this.limit = limit;
        this.windowMillis = window.toMillis();
        this.serviceTime = serviceTime;
    }

    /**
     * @return 이번 시간 창에서 limit 안이면 true
     */
    public boolean tryAcquire(String ip) {
        resetIfNewWindow();
        return counts.computeIfAbsent(ip, key -> new AtomicInteger()).incrementAndGet() <= limit;
    }

    private synchronized void resetIfNewWindow() {
        long window = serviceTime.now().toEpochMilli() / windowMillis;
        if (window != currentWindow) {
            counts.clear();
            currentWindow = window;
        }
    }
}
