package com.ject.exchange.domain.exception;

import com.ject.common.exception.ErrorCode;

public enum ExchangeErrorCode implements ErrorCode {

    EXCHANGE_NOT_FOUND("EXCHANGE_001", 404, "교환을 찾을 수 없습니다."),
    NOT_EXCHANGE_PARTICIPANT("EXCHANGE_002", 403, "교환 참여자가 아닙니다."),
    OWN_POST_REQUEST("EXCHANGE_003", 400, "본인 글에는 교환을 요청할 수 없습니다."),
    POST_NOT_REQUESTABLE("EXCHANGE_005", 409, "교환을 요청할 수 없는 글입니다."),
    POST_NOT_ATTACHABLE("EXCHANGE_007", 409, "첨부할 수 없는 글이 포함되어 있습니다."),
    BLOCKED_USER("EXCHANGE_008", 403, "차단 관계인 사용자입니다."),
    POST_NOT_FOUND("EXCHANGE_009", 404, "교환글을 찾을 수 없습니다."),
    EXCHANGE_CLOSED("EXCHANGE_010", 409, "종료된 교환입니다."),
    LEFT_CHAT_ROOM_REQUEST("EXCHANGE_011", 409, "이미 나간 채팅방의 글에는 다시 요청할 수 없습니다."),
    NOT_REQUESTER("EXCHANGE_012", 403, "요청자가 아닙니다."),
    NOT_NEW_ITEM_OFFER("EXCHANGE_013", 409, "새로 작성 방식의 요청이 아닙니다."),
    NEW_ITEM_LOCKED("EXCHANGE_014", 409, "확정된 약속이 있어 아이템을 수정할 수 없습니다."),

    APPOINTMENT_NOT_FOUND("EXCHANGE_101", 404, "약속을 찾을 수 없습니다."),
    INVALID_SCHEDULED_AT("EXCHANGE_102", 400, "약속 시각이 올바르지 않습니다."),
    APPOINTMENT_ALREADY_EXISTS("EXCHANGE_103", 409, "진행 중인 제안 또는 확정된 약속이 이미 있습니다."),
    NOT_APPOINTMENT_RECEIVER("EXCHANGE_104", 403, "제안받은 사람이 아닙니다."),
    NOT_APPOINTMENT_PROPOSER("EXCHANGE_105", 403, "제안한 사람이 아닙니다."),
    PROPOSAL_NOT_ACTIVE("EXCHANGE_106", 409, "유효하지 않은 제안입니다."),
    SCHEDULED_AT_PASSED("EXCHANGE_107", 409, "약속 시각이 이미 지났습니다."),
    POST_RESERVED_ELSEWHERE("EXCHANGE_108", 409, "다른 분과 약속이 잡혀 있는 글입니다."),
    INVALID_APPOINTMENT_STATUS("EXCHANGE_109", 409, "처리할 수 없는 약속 상태입니다."),
    REMINDER_NOT_ALLOWED("EXCHANGE_110", 409, "지금 바로 약속은 알림을 설정할 수 없습니다."),
    INVALID_OFFERED_ITEMS("EXCHANGE_111", 400, "교환할 물건 지정이 올바르지 않습니다."),
    ITEM_NOT_AVAILABLE("EXCHANGE_112", 409, "교환할 수 있는 아이템이 아닙니다."),

    CHANGE_NOT_FOUND("EXCHANGE_121", 404, "변경 제안을 찾을 수 없습니다."),
    CHANGE_ALREADY_PENDING("EXCHANGE_122", 409, "대기 중인 변경 제안이 이미 있습니다."),
    CHANGE_TIME_PASSED("EXCHANGE_123", 409, "약속 시각이 지나 변경할 수 없습니다."),
    NOT_CHANGE_RECEIVER("EXCHANGE_124", 403, "변경 제안을 받은 사람이 아닙니다."),
    CHANGE_NOT_PENDING("EXCHANGE_125", 409, "이미 처리되었거나 만료된 변경 제안입니다."),

    CANCELLATION_NOT_FOUND("EXCHANGE_201", 404, "취소 요청을 찾을 수 없습니다."),
    CANCELLATION_ALREADY_PENDING("EXCHANGE_202", 409, "진행 중인 취소 요청이 이미 있습니다."),
    NOT_CANCELLATION_RECEIVER("EXCHANGE_203", 403, "취소 요청을 받은 사람이 아닙니다."),
    NOT_CANCELLATION_REQUESTER("EXCHANGE_204", 403, "취소 요청자가 아닙니다."),
    CANCELLATION_ALREADY_RESOLVED("EXCHANGE_205", 409, "이미 처리된 취소 요청입니다."),
    FORCE_CANCEL_TOO_EARLY("EXCHANGE_206", 409, "아직 일방 취소할 수 없습니다."),

    CONFIRMATION_NOT_OPEN("EXCHANGE_301", 409, "아직 교환 확인에 응답할 수 없습니다."),
    CONFIRMATION_ALREADY_ANSWERED("EXCHANGE_302", 409, "이미 교환 확인에 응답했습니다."),
    CONFIRMATION_ALREADY_DECIDED("EXCHANGE_303", 409, "이미 결과가 정해진 교환 확인입니다.");

    private final String code;
    private final int status;
    private final String message;

    ExchangeErrorCode(String code, int status, String message) {
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
