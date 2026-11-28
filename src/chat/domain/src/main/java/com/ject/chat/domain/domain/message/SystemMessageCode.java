package com.ject.chat.domain.domain.message;

public enum SystemMessageCode {

    /**
     * 다른 곳에서 먼저 약속이 잡혀 이 제안이 자동으로 막힘
     */
    APPOINTMENT_RESERVED_ELSEWHERE,

    /**
     * 선점이 풀려 약속 제안을 다시 할 수 있음
     */
    APPOINTMENT_REOPENED,

    /**
     * 상대가 약속 제안을 거절함
     */
    PROPOSAL_REJECTED,

    /**
     * 약속 제안이 시스템에 의해 자동으로 거절됨
     */
    PROPOSAL_AUTO_REJECTED,

    /**
     * 약속 제안이 응답 시한을 넘겨 만료됨
     */
    PROPOSAL_EXPIRED,

    /**
     * 약속 시간·장소가 변경됨
     */
    APPOINTMENT_CHANGED,

    /**
     * 약속 변경 요청이 거절됨
     */
    APPOINTMENT_CHANGE_REJECTED,

    /**
     * 약속이 취소됨
     */
    APPOINTMENT_CANCELED,

    /**
     * 내가 첨부한 교환글이 수정됨
     */
    ATTACHED_POST_UPDATED,

    /**
     * 상대가 새로 작성한 아이템 내용이 수정됨
     */
    NEW_ITEM_UPDATED,

    /**
     * 상대가 요청한 대상 교환글이 수정됨
     */
    TARGET_POST_UPDATED,

    /**
     * 상대가 요청한 대상 교환글이 삭제됨
     */
    TARGET_POST_DELETED,

    /**
     * 같은 글을 두고 경쟁하던 다른 교환이 완료되어 이 교환은 더 진행할 수 없음
     */
    OTHER_EXCHANGE_COMPLETED,

    /**
     * 상대가 채팅방을 나감
     */
    COUNTERPART_LEFT
}
