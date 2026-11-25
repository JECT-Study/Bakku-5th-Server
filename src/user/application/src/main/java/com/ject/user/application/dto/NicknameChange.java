package com.ject.user.application.dto;

import java.time.Instant;

/**
 * 닉네임 수정 결과 (MB-3).
 *
 * @param nicknameChangeableAt 다음에 닉네임을 바꿀 수 있는 시각 (변경 시각 + 30일)
 */
public record NicknameChange(String nickname, Instant nicknameChangeableAt) {
}
