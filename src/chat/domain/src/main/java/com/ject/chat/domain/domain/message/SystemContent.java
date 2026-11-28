package com.ject.chat.domain.domain.message;

/**
 * SYSTEM 메시지 내용 (code·서버 문구 필수, link는 선택이지만 있으면 종류·ID가 함께 있어야 함)
 */
public record SystemContent(SystemMessageCode code, String text, SystemLinkType linkType, Long linkId)
        implements ChatMessageContent {

    public SystemContent {
        if (code == null || text == null || text.isBlank()) {
            throw new IllegalArgumentException("SYSTEM 메시지는 code와 서버 문구가 필요합니다.");
        }
        if ((linkType == null) != (linkId == null)) {
            throw new IllegalArgumentException("SYSTEM 메시지의 link는 종류와 ID를 함께 가지거나 함께 없어야 합니다.");
        }
    }
}
