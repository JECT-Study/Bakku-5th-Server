package com.ject.exchange.application.contract;

/**
 * chat BC가 채팅방 상세에서 필요로 하는 약속 상태
 */
public enum ChatRoomAppointmentState {
    /** 현재 유효한 확정 약속이 없음 */
    NONE,
    /** 확정 약속이 있고 교환 확인 질문이 열리기 전 */
    CONFIRMED,
    /** 교환 확인 질문이 열린 뒤 */
    CONFIRMATION_OPENED
}
