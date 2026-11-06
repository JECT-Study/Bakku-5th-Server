package com.ject.chat.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "타입별 내용 (MessageType에 따라 형태가 다름)", oneOf = {
        TextMessageContent.class, ImageMessageContent.class, PlaceMessageContent.class,
        SystemMessageContent.class, CardMessageContent.class})
public sealed interface MessageContent
        permits TextMessageContent, ImageMessageContent, PlaceMessageContent, SystemMessageContent, CardMessageContent {
}
