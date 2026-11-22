package com.ject.user.application.dto;

import java.time.Instant;
import java.util.List;

/**
 * 차단 목록 한 페이지 (BL-2).
 *
 * @param nextCursorId 다음 페이지 커서(마지막 차단 id). 마지막 페이지면 null
 */
public record BlockedMemberPage(List<BlockedMember> content, Long nextCursorId, boolean hasNext) {

    public record BlockedMember(Long userId, String nickname, Instant blockedAt) {
    }
}
