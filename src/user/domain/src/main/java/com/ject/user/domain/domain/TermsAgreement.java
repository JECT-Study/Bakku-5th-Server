package com.ject.user.domain.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * 가입 때 받은 약관 동의 한 건. 동의 일시 · 약관 버전을 남긴다 (A-04-1).
 */
public record TermsAgreement(TermsType type, String version, boolean agreed, Instant agreedAt) {

    public TermsAgreement {
        Objects.requireNonNull(type);
        Objects.requireNonNull(version);
        Objects.requireNonNull(agreedAt);
    }
}
