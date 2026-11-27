package com.ject.chat.application.dto;

/**
 * 입력창을 막는 사유. 겹치면 선언 순서를 우선순위로 하나만 반환한다 (spec 용어와 판정 우선순위)
 */
public enum ChatInputDisabledReason {
    /** 양방향 중 하나라도 차단 관계 */
    BLOCKED,
    /** 상대가 탈퇴 또는 영구 정지됨 */
    COUNTERPART_WITHDRAWN,
    /** 상대가 이용 제한 중 */
    COUNTERPART_RESTRICTED,
    /** 조회 회원이 이용 제한 중 */
    MEMBER_RESTRICTED,
    /** 상대 참여자가 방을 나감 */
    COUNTERPART_LEFT,
    /** 대상 게시글이 이 방이 아닌 다른 교환에서 완료됨 */
    OTHER_EXCHANGE_COMPLETED
}
