package com.ject.chat.application.dto;

public enum ChatRoomLinkStatus {
    /** 요청자가 이 게시글로 만든 방이 없음 */
    ABSENT,
    /** 요청자가 나가지 않아 다시 들어갈 수 있는 현재 방 */
    ACTIVE,
    /** 요청자가 나간 이력이 있는 방. 재요청을 허용하지 않는다 */
    LEFT
}
