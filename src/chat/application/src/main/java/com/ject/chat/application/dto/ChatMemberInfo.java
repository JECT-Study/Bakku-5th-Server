package com.ject.chat.application.dto;

/**
 * chat BC가 필요로 하는 회원 요약. user BC {@code MemberSummary} 계약을 이 형태로 변환한다.
 *
 * @param userId         회원 ID
 * @param nickname       닉네임. 탈퇴한 회원이면 "탈퇴한 사용자"
 * @param completedCount 교환 완료 횟수
 * @param withdrawn      탈퇴 또는 영구 정지 여부
 */
public record ChatMemberInfo(Long userId, String nickname, int completedCount, boolean withdrawn) {
}
