package com.ject.chat.application.dto;

/**
 * chat BC가 필요로 하는 약속 상태. exchange BC {@code ChatRoomExchangeState.appointmentState}를 이 형태로 변환한다.
 */
public enum ChatAppointmentState {
    /** 현재 유효한 확정 약속이 없음 */
    NONE,
    /** 확정 약속이 있고 교환 확인 질문이 열리기 전 */
    CONFIRMED,
    /** 교환 확인 질문이 열린 뒤 */
    CONFIRMATION_OPENED
}
