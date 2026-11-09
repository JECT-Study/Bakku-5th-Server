package com.ject.common.event.member;

import java.time.Instant;

/**
 * 회원 차단. 교환은 예약중 약속을 취소하고 채팅은 채팅방을 숨긴다 (결정 B4).
 */
public record MemberBlocked(Long blockerId, Long blockedId, Instant blockedAt) {
}
