package com.ject.chat.domain.exception;

import com.ject.common.exception.ErrorCode;

public enum ChatErrorCode implements ErrorCode {

    CHAT_ROOM_NOT_FOUND("CHAT_001", 404, "채팅방을 찾을 수 없습니다."),
    NOT_CHAT_PARTICIPANT("CHAT_002", 403, "채팅방 참여자가 아닙니다."),
    OTHER_EXCHANGE_COMPLETED("CHAT_003", 409, "다른 분과 교환이 완료되어 메시지를 보낼 수 없습니다."),
    COUNTERPART_LEFT("CHAT_004", 409, "상대가 대화방을 나갔습니다."),
    BLOCKED_USER("CHAT_005", 403, "차단한 사용자와는 대화할 수 없습니다."),
    UNSENDABLE_MESSAGE_TYPE("CHAT_006", 400, "보낼 수 없는 메시지 타입입니다."),
    MESSAGE_NOT_IN_ROOM("CHAT_007", 400, "이 채팅방의 메시지가 아닙니다."),
    ALREADY_LEFT("CHAT_008", 409, "이미 나간 채팅방입니다.");

    private final String code;
    private final int status;
    private final String message;

    ChatErrorCode(String code, int status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public int getStatus() {
        return status;
    }
}
