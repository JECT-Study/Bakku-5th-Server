package com.ject.chat.application.dto;

/**
 * 채팅방 나가기가 약속 · 교환에 주는 영향. FE가 나가기 확인 팝업 종류를 고르는 데 쓴다
 */
public enum ChatRoomLeaveImpact {
    /** 현재 확정 약속이 없음 */
    NONE,
    /** 확정 약속이 있고 교환 확인 질문이 열리기 전 */
    UNILATERAL_CANCEL,
    /** 교환 확인 질문이 열린 뒤 */
    COMPLETE_EXCHANGE
}
