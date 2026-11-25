package com.ject.user.domain.exception;

import com.ject.common.core.exception.DomainException;

import java.time.Instant;

/**
 * 마지막 닉네임 변경 뒤 30일 안에 다시 변경 (MEMBER_103). FE는 MB-2 nicknameChangeableAt으로 날짜를 안내한다 (G-03-2).
 */
public class NicknameChangeTooSoonException extends DomainException {

    public NicknameChangeTooSoonException(Instant changeableAt) {
        super(UserErrorCode.NICKNAME_CHANGE_TOO_SOON, "닉네임은 30일에 한 번 바꿀 수 있습니다. changeableAt=" + changeableAt);
    }
}
