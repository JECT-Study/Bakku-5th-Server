package com.ject.user.application.dto;

import com.ject.user.domain.domain.MannerTier;
import com.ject.user.domain.domain.Member;

/**
 * 다른 BC에 주는 회원 요약. 교환 · 채팅 응답의 requester · owner · counterpart가 이 값을 쓴다 (결정 B8).
 * 탈퇴 · 영구 정지 회원은 withdrawn = true, nickname = "탈퇴한 사용자" (REQ-N-11, 명세 MemberStatus.BANNED).
 *
 * @param mannerTier 매너 지표 구간 이름 (PRO_COLLECTOR · GOODS_HUNTER · TRAINEE). 숫자는 다른 사람에게 보이지 않는다
 *                   (F-06-1, 11/24). 교환 0회 · 탈퇴 회원이면 null
 */
public record MemberSummary(Long userId, String nickname, int completedCount, boolean withdrawn, String mannerTier) {

    public static final String WITHDRAWN_NICKNAME = "탈퇴한 사용자";

    /**
     * 매너 지표를 쓰지 않는 곳(테스트 등)에서 쓴다.
     */
    public MemberSummary(Long userId, String nickname, int completedCount, boolean withdrawn) {
        this(userId, nickname, completedCount, withdrawn, null);
    }

    public static MemberSummary from(Member member) {
        if (member.appearsWithdrawn()) {
            return new MemberSummary(member.getId(), WITHDRAWN_NICKNAME, member.getCompletedCount(), true, null);
        }
        MannerTier tier = member.mannerTier();
        return new MemberSummary(member.getId(), member.getNickname().value(), member.getCompletedCount(), false,
                tier == null ? null : tier.name());
    }
}
