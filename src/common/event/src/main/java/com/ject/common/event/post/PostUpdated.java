package com.ject.common.event.post;

import java.time.Instant;

// 교환글 수정 (사진 포함 내용이 바뀐 경우만). 받는 쪽(채팅 수정 자동 메시지 · 알림)은 커밋 뒤 처리
public record PostUpdated(Long postId, Long authorId, String title, Instant updatedAt) {
}
