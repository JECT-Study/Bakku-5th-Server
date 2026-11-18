package com.ject.user.domain.domain;

import java.time.LocalDate;

/**
 * 현재 시행 중인 약관 한 건 (A-04-1, G-10-1, 결정 A20).
 *
 * @param documentPath 본문 웹뷰 경로 이름. 만 14세 확인처럼 본문이 없으면 null
 */
public record Terms(TermsType type, String title, String version, TermsConsent signupConsent,
                    String documentPath, LocalDate effectiveAt) {
}
