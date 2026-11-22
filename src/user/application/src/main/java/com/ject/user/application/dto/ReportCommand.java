package com.ject.user.application.dto;

/**
 * 신고 (RP-1).
 *
 * @param targetType ReportTargetType 이름
 * @param reason     ReportReason 이름
 * @param detail     기타 사유일 때만 쓰고, 아니면 무시
 */
public record ReportCommand(Long reporterId, String targetType, Long targetId, String reason, String detail) {
}
