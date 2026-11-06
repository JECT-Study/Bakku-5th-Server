package com.ject.user.domain.exception;

import com.ject.common.core.exception.CommonErrorCode;
import com.ject.common.core.exception.DomainException;

/**
 * 미래 날짜나 1900-01-01 이전처럼 생년월일로 볼 수 없는 값. 만 14세 미만(MEMBER_013)과 구분해 입력값 오류로 응답한다.
 */
public class InvalidBirthDateException extends DomainException {

    public InvalidBirthDateException(String message) {
        super(CommonErrorCode.INVALID_INPUT, message);
    }
}
