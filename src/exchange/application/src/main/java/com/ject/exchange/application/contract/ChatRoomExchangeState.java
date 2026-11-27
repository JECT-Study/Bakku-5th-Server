package com.ject.exchange.application.contract;

/**
 * chat BC가 채팅방 상세 판정에 필요로 하는 교환 상태.
 *
 * @param otherExchangeCompleted 대상 게시글이 조회한 교환이 아닌 다른 교환에서 완료됐는지 (이 교환의 완료 여부가 아니다)
 * @param appointmentState       현재 유효한 확정 약속과 교환 확인 질문 개시 여부
 */
public record ChatRoomExchangeState(boolean otherExchangeCompleted, ChatRoomAppointmentState appointmentState) {
}
