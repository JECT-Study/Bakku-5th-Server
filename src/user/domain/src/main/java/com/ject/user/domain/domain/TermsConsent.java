package com.ject.user.domain.domain;

/**
 * 가입 때 동의를 받는 방식. NONE은 동의 없이 보기만 하는 약관(개인정보 처리방침).
 */
public enum TermsConsent {
    REQUIRED,
    OPTIONAL,
    NONE
}
