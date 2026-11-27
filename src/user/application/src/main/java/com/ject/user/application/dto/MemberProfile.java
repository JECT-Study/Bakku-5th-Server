package com.ject.user.application.dto;

import com.ject.user.domain.domain.MannerTier;
import com.ject.user.domain.domain.Member;

/**
 * 다른 회원 프로필 (MB-4). 닉네임 · 교환 완료 횟수 · 매너 지표 구간 이름만 공개한다 (P-04-03, REQ-N-02).
 *
 * @param mannerTier 매너 지표 구간 이름 (PRO_COLLECTOR · GOODS_HUNTER · TRAINEE), 교환 0회면 null
 */
public record MemberProfile(Long userId, String nickname, int completedCount, boolean isMe, String mannerTier) {

    public static MemberProfile of(Member member, boolean isMe) {
        MannerTier tier = member.mannerTier();
        return new MemberProfile(member.getId(), member.getNickname().value(), member.getCompletedCount(), isMe,
                tier == null ? null : tier.name());
    }
}
