package com.ject.user.application.dto;

import java.time.Instant;

/**
 * @param created 새로 차단했으면 true, 이미 차단한 회원이면 false (blockedAt은 기존 차단 시각)
 */
public record BlockResult(Long userId, Instant blockedAt, boolean created) {
}
