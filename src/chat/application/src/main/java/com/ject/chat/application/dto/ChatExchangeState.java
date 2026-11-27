package com.ject.chat.application.dto;

/**
 * chat BC가 필요로 하는 교환 상태.
 *
 * @param otherExchangeCompleted 이 방의 대상 게시글이 이 방이 아닌 다른 교환에서 완료됐는지
 * @param appointmentState       현재 유효한 확정 약속과 교환 확인 질문 개시 여부
 */
public record ChatExchangeState(boolean otherExchangeCompleted, ChatAppointmentState appointmentState) {
}
