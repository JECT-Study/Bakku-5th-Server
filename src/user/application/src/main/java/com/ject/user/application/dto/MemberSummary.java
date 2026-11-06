package com.ject.user.application.dto;

import com.ject.user.domain.domain.Member;

/**
 * 다른 BC에 주는 회원 요약. 교환 · 채팅 응답의 requester · owner · counterpart가 이 값을 쓴다 (결정 B8).
 * 탈퇴 · 영구 정지 회원은 withdrawn = true, nickname = "탈퇴한 사용자" (REQ-N-11, 명세 MemberStatus.BANNED).
 */
public record MemberSummary(Long userId, String nickname, int completedCount, boolean withdrawn) {

    public static final String WITHDRAWN_NICKNAME = "탈퇴한 사용자";

    public static MemberSummary from(Member member) {
        if (member.appearsWithdrawn()) {
            return new MemberSummary(member.getId(), WITHDRAWN_NICKNAME, member.getCompletedCount(), true);
        }
        return new MemberSummary(member.getId(), member.getNickname().value(), member.getCompletedCount(), false);
    }
}
