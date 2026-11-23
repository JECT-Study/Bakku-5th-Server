package com.ject.common.event.post;

import java.time.Instant;

// 교환글 삭제. 받는 쪽(채팅 시스템 메시지 · 교환 제안 취소)은 커밋 뒤 처리
public record PostDeleted(Long postId, Long authorId, Instant deletedAt) {
}
