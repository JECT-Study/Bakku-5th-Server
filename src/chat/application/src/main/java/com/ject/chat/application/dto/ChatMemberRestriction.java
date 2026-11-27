package com.ject.chat.application.dto;

/**
 * chat BC가 필요로 하는 이용 제한 여부. 제한 해제 일시 등 CH-2 응답에 없는 개인정보는 담지 않는다.
 *
 * @param restricted 이용 제한 중인지
 */
public record ChatMemberRestriction(boolean restricted) {
}
