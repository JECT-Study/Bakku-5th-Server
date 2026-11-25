package com.ject.user.application.dto;

import java.time.Instant;

/**
 * 회원의 이용 제한 상태. 제한 중이면 보기만 할 수 있고 게시글 작성 · 교환 요청 · 메시지 · 약속이 막힌다 (P-05-01).
 *
 * @param reason          제한 사유. 사유를 저장하는 제재 기록이 생기기 전이라 지금은 null
 * @param restrictedUntil 제한 해제 일시, 제한 중이 아니면 null
 */
public record MemberRestriction(boolean restricted, RestrictionReason reason, Instant restrictedUntil) {

    private static final MemberRestriction NONE = new MemberRestriction(false, null, null);

    public static MemberRestriction none() {
        return NONE;
    }

    /**
     * 운영자가 이용 제한을 걸 때 고르는 사유 (H-03-2). 금지어 반복 · 허위 신고는 출시 후라 없다.
     */
    public enum RestrictionReason {
        /** 노쇼 누적 */
        NO_SHOW,
        /** 외부 거래 */
        EXTERNAL_TRADE,
        /** 판매 */
        SELLING,
        /** 기타 */
        OTHER
    }
}
