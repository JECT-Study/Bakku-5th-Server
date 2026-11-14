package com.ject.user.domain.domain;

/**
 * 약관 종류. LOCATION은 1차 가입 약관에 넣지 않고 정의만 둔다 (결정 B13).
 */
public enum TermsType {
    SERVICE,
    PRIVACY_COLLECTION,
    AGE_14,
    MARKETING,
    PRIVACY_POLICY,
    LOCATION
}
