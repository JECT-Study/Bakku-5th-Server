package com.ject.exchange.infrastructure.adapter;

import com.ject.common.core.exception.DomainException;
import com.ject.common.core.exception.ErrorCode;

/**
 * user-domain의 MemberNotFoundException은 exchange 테스트 클래스패스에 없어 같은 성격(DomainException 하위)의 대역을 쓴다.
 */
class TestMemberNotFoundException extends DomainException {

    TestMemberNotFoundException() {
        super(TestErrorCode.MEMBER_NOT_FOUND);
    }

    enum TestErrorCode implements ErrorCode {
        MEMBER_NOT_FOUND;

        @Override
        public String getCode() {
            return "MEMBER_201";
        }

        @Override
        public String getMessage() {
            return "회원을 찾을 수 없습니다.";
        }

        @Override
        public int getStatus() {
            return 404;
        }
    }
}
