package com.ject.chat.application.dto;

/**
 * CH-2 채팅방 상세 조회 결과. api enum에 의존하지 않고, api Response가 {@code EnumMapper}로 변환한다.
 *
 * @param chatRoomId          채팅방 ID
 * @param exchangeId          연결된 교환 ID
 * @param status              입력 가능 상태
 * @param inputDisabledReason 입력창을 막는 사유. 입력 가능하면 null
 * @param counterpart         상대 정보
 * @param notificationEnabled 조회 회원의 이 방 알림 설정
 * @param leaveImpact         나가기 확인 팝업 종류
 * @param myLastReadMessageId 조회 회원이 마지막으로 읽은 메시지 ID. 없으면 null
 */
public record ChatRoomDetailResult(
        Long chatRoomId,
        Long exchangeId,
        ChatRoomAccessStatus status,
        ChatInputDisabledReason inputDisabledReason,
        CounterpartDetail counterpart,
        boolean notificationEnabled,
        ChatRoomLeaveImpact leaveImpact,
        Long myLastReadMessageId
) {
}
