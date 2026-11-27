package com.ject.chat.application.dto;

/**
 * 채팅방 상세의 상대 정보.
 *
 * @param userId         상대 회원 ID
 * @param nickname       상대 닉네임. 탈퇴한 회원이면 user BC 계약의 "탈퇴한 사용자"
 * @param completedCount 상대의 교환 완료 횟수
 * @param left           상대 참여자가 이 방을 나갔는지
 */
public record CounterpartDetail(Long userId, String nickname, int completedCount, boolean left) {
}
