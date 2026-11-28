package com.ject.chat.application.dto;

/**
 * @param linkType [보기] 버튼 대상 종류. 없으면 null
 * @param linkId   [보기] 버튼 대상 ID. 없으면 null
 */
public record SystemMessageContentResult(ChatMessageSystemCode code, String text,
                                         ChatMessageSystemLinkType linkType, Long linkId)
        implements ChatMessageContentResult {
}
