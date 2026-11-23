package com.ject.post.application.contract;

// status: AVAILABLE · RESERVED · COMPLETED · DELETED, thumbnailKey: 대표 사진(첫 장) key
public record PostBrief(Long id, Long authorId, String status, String title, String thumbnailKey) {
}
