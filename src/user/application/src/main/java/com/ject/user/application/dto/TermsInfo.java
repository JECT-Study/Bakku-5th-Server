package com.ject.user.application.dto;

import com.ject.user.domain.domain.Terms;

import java.time.LocalDate;

/**
 * 약관 목록 조회 결과. termsType · signupConsent는 enum 이름이다.
 *
 * @param documentPath 본문 웹뷰 경로 이름. 본문이 없으면 null
 */
public record TermsInfo(String termsType, String title, String version, String signupConsent,
                        String documentPath, LocalDate effectiveAt) {

    public static TermsInfo from(Terms terms) {
        return new TermsInfo(terms.type().name(), terms.title(), terms.version(), terms.signupConsent().name(),
                terms.documentPath(), terms.effectiveAt());
    }
}
