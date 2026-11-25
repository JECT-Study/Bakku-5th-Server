package com.ject.user.application.dto;

import com.ject.user.domain.domain.Member;

import java.time.Instant;
import java.time.LocalDate;

/**
 * 내 정보 (MB-2). 마이페이지 요약(G-01-1) · 내 프로필(G-03-1)에 쓴다.
 *
 * @param provider             가입에 쓴 소셜 계정 (KAKAO · NAVER · APPLE)
 * @param gender               선택 입력이라 null 가능
 * @param nicknameChangeableAt 지금 닉네임을 바꿀 수 있으면 null
 */
public record MyInfo(
        Long userId,
        String nickname,
        String provider,
        LocalDate birthDate,
        String gender,
        int completedCount,
        Instant nicknameChangeableAt,
        boolean marketingAgreed,
        Instant createdAt
) {

    public static MyInfo from(Member member, Instant now) {
        return new MyInfo(
                member.getId(),
                member.getNickname().value(),
                member.getProvider().name(),
                member.getBirthDate().value(),
                member.getGender() == null ? null : member.getGender().name(),
                member.getCompletedCount(),
                member.nicknameChangeableAt(now),
                member.isMarketingAgreed(),
                member.getCreatedAt()
        );
    }
}
