package com.ject.exchange.application.dto.dto;

public record UserInfo(
        Long id,
        String nickname,        // 탈퇴 · 영구 정지 회원은 "탈퇴한 사용자"
        int completedCount
) {}
