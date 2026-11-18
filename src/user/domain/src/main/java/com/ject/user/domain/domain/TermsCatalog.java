package com.ject.user.domain.domain;

import java.time.LocalDate;
import java.util.List;

/**
 * 현재 시행 중인 약관 목록. 약관을 개정하면 이 목록의 버전 · 시행일을 바꿔 배포한다.
 * 가입 때 받은 약관 버전은 이 목록과 비교한다 (A-04-1 "약관 버전 저장").
 */
public final class TermsCatalog {

    private static final String VERSION = "1.0";
    private static final LocalDate EFFECTIVE_AT = LocalDate.of(2026, 11, 27);

    private static final List<Terms> CURRENT = List.of(
            new Terms(TermsType.SERVICE, "서비스 이용약관", VERSION, TermsConsent.REQUIRED,
                    "service", EFFECTIVE_AT),
            new Terms(TermsType.PRIVACY_COLLECTION, "개인정보 수집 · 이용 동의", VERSION, TermsConsent.REQUIRED,
                    "privacy-collection", EFFECTIVE_AT),
            new Terms(TermsType.AGE_14, "만 14세 이상입니다", VERSION, TermsConsent.REQUIRED,
                    null, EFFECTIVE_AT),
            new Terms(TermsType.MARKETING, "마케팅 정보 수신 동의 (선택)", VERSION, TermsConsent.OPTIONAL,
                    "marketing", EFFECTIVE_AT),
            new Terms(TermsType.PRIVACY_POLICY, "개인정보 처리방침", VERSION, TermsConsent.NONE,
                    "privacy-policy", EFFECTIVE_AT)
    );

    private TermsCatalog() {
    }

    /**
     * 화면에 보일 순서대로 반환한다.
     */
    public static List<Terms> current() {
        return CURRENT;
    }
}
