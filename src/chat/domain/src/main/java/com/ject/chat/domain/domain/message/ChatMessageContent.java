package com.ject.chat.domain.domain.message;

/**
 * 메시지 타입별 내용. 한 메시지는 자신의 타입에 필요한 내용만 가진다
 */
public sealed interface ChatMessageContent permits TextContent, ImageContent, SystemContent, CardContent {
}
