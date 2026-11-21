package com.ject.user.domain.exception;

import com.ject.common.core.exception.DomainException;

/**
 * 가입 약관 동의가 올바르지 않을 때. 필수 약관 미동의(MEMBER_011) · 최신이 아닌 약관 버전(MEMBER_012).
 */
public class TermsAgreementException extends DomainException {

    private TermsAgreementException(UserErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public static TermsAgreementException requiredNotAgreed(String termsType) {
        return new TermsAgreementException(UserErrorCode.REQUIRED_TERMS_NOT_AGREED,
                "필수 약관에 동의하지 않았습니다. termsType=" + termsType);
    }

    public static TermsAgreementException outdatedVersion(String termsType) {
        return new TermsAgreementException(UserErrorCode.TERMS_VERSION_OUTDATED,
                "약관 버전이 최신이 아닙니다. termsType=" + termsType);
    }
}
