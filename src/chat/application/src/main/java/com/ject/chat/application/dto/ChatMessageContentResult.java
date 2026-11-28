package com.ject.chat.application.dto;

/**
 * 메시지 타입별 결과 내용. domain content를 API에 노출하지 않고 application 전용 표현으로 옮긴다
 */
public sealed interface ChatMessageContentResult
        permits TextMessageContentResult, ImageMessageContentResult, SystemMessageContentResult,
        CardMessageContentResult {
}
