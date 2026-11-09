package com.ject.common.event.member;

import java.time.Instant;

/**
 * 회원 탈퇴. 교환 → 교환글 → 채팅 순서로 처리한다 (결정 B6).
 */
public record MemberWithdrawn(Long memberId, Instant withdrawnAt) {
}
