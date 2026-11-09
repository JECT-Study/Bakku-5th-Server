package com.ject.common.event.member;

/**
 * 회원 차단 해제. 숨긴 채팅방을 다시 보이게 하고, 취소된 약속은 되살리지 않는다 (결정 B4).
 */
public record MemberUnblocked(Long blockerId, Long blockedId) {
}
