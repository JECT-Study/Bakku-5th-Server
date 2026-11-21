package com.ject.user.application.dto;

import com.ject.user.domain.domain.Member;

import java.time.Instant;

/**
 * 마케팅 정보 수신 동의 (MB-7).
 *
 * @param changedAt 마지막으로 동의 · 철회한 시각
 */
public record MarketingConsent(boolean agreed, Instant changedAt) {

    public static MarketingConsent from(Member member) {
        return new MarketingConsent(member.isMarketingAgreed(), member.getMarketingChangedAt());
    }
}
