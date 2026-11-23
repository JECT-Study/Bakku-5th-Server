package com.ject.notification.application.port;

/**
 * 푸시 제목에 넣을 보낸 회원 닉네임 조회 (회원 BC). 탈퇴했으면 "탈퇴한 사용자"다.
 */
public interface MemberNicknamePort {

    String getNickname(Long memberId);
}
