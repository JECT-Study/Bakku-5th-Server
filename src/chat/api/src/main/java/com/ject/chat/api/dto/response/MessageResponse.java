package com.ject.chat.api.dto.response;

import com.ject.chat.api.dto.enums.CardType;
import com.ject.chat.api.dto.enums.MessageType;
import com.ject.chat.api.dto.enums.SystemLinkType;
import com.ject.chat.api.dto.enums.SystemMessageCode;
import com.ject.chat.application.dto.CardMessageContentResult;
import com.ject.chat.application.dto.ChatMessageContentResult;
import com.ject.chat.application.dto.ChatMessageResult;
import com.ject.chat.application.dto.ImageMessageContentResult;
import com.ject.chat.application.dto.SystemMessageContentResult;
import com.ject.chat.application.dto.TextMessageContentResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Schema(description = "메시지")
public record MessageResponse(
        @Schema(description = "메시지 ID (방 안에서 시간순 증가)", example = "121")
        Long messageId,

        @Schema(description = "채팅방 ID", example = "5")
        Long chatRoomId,

        @Schema(description = "보낸 사람. 시스템 · 카드 메시지는 null", example = "101")
        Long senderUserId,

        @Schema(description = "메시지 타입", example = "TEXT")
        MessageType type,

        @Schema(description = "타입별 내용")
        MessageContent content,

        @Schema(description = "보낸 쪽이 만든 ID. 시스템 · 카드 메시지는 null", example = "2f1c9a7e-5b1d-4c0e-9e8f-0a1b2c3d4e5f")
        String clientMessageId,

        @Schema(description = "보낸 시각", example = "2026-10-12T14:31:00+09:00")
        OffsetDateTime sentAt
) {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    public static MessageResponse from(ChatMessageResult result) {
        return new MessageResponse(
                result.messageId(),
                result.chatRoomId(),
                result.senderUserId(),
                EnumMapper.map(result.type(), MessageType.class),
                toContent(result.content()),
                result.clientMessageId(),
                toSeoul(result.sentAt())
        );
    }

    private static MessageContent toContent(ChatMessageContentResult content) {
        return switch (content) {
            case TextMessageContentResult text ->
                    new TextMessageContent(text.text(), text.externalTradeDetected());
            case ImageMessageContentResult image -> new ImageMessageContent(image.imageUrls());
            case SystemMessageContentResult system -> new SystemMessageContent(
                    EnumMapper.map(system.code(), SystemMessageCode.class),
                    system.text(),
                    system.linkType() == null ? null : new SystemMessageLink(
                            EnumMapper.map(system.linkType(), SystemLinkType.class), system.linkId()));
            case CardMessageContentResult card -> new CardMessageContent(
                    EnumMapper.map(card.cardType(), CardType.class), card.targetId());
        };
    }

    private static OffsetDateTime toSeoul(Instant instant) {
        return instant == null ? null : instant.atZone(SEOUL).toOffsetDateTime();
    }
}
