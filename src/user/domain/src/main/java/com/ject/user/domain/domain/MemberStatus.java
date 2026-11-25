package com.ject.user.domain.domain;

public enum MemberStatus {
    /** 이용 중 */
    ACTIVE,
    /** 기간 이용 제한. 보기만 할 수 있고 게시글 작성 · 교환 요청 · 메시지 · 약속이 막힌다 (P-05-01) */
    RESTRICTED,
    /** 영구 정지. 로그인할 수 없고 상대 화면에는 탈퇴한 회원과 같게 보인다 (A-03-1, P-05-01) */
    BANNED,
    /** 탈퇴. 닉네임 · 생년월일 · 성별을 비운다 (G-09-2) */
    WITHDRAWN
}
