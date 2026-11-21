package com.ject.user.domain.domain;

import com.ject.user.domain.exception.TermsAgreementException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
     * 가입 요청에 담긴 약관 하나의 버전 · 동의 여부.
     */
    public record SignupConsent(String version, boolean agreed) {
    }

    /**
     * 화면에 보일 순서대로 반환한다.
     */
    public static List<Terms> current() {
        return CURRENT;
    }

    /**
     * 가입 때 받은 동의를 확인하고, 저장할 동의 목록을 만든다.
     * 동의를 받는 약관(필수 · 선택)은 모두 현재 버전이어야 하고(MEMBER_012), 필수 약관은 동의해야 한다(MEMBER_011).
     * 보기만 하는 약관 · 목록에 없는 약관은 저장하지 않는다.
     *
     * @param consents 약관 종류별로 FE가 보낸 버전 · 동의 여부
     */
    public static List<TermsAgreement> signupAgreements(Map<TermsType, SignupConsent> consents, Instant agreedAt) {
        List<TermsAgreement> agreements = new ArrayList<>();
        for (Terms terms : CURRENT) {
            if (terms.signupConsent() == TermsConsent.NONE) {
                continue;
            }
            SignupConsent consent = consents.get(terms.type());
            if (consent == null) {
                if (terms.signupConsent() == TermsConsent.REQUIRED) {
                    throw TermsAgreementException.requiredNotAgreed(terms.type().name());
                }
                continue;
            }
            if (!terms.version().equals(consent.version())) {
                throw TermsAgreementException.outdatedVersion(terms.type().name());
            }
            if (terms.signupConsent() == TermsConsent.REQUIRED && !consent.agreed()) {
                throw TermsAgreementException.requiredNotAgreed(terms.type().name());
            }
            agreements.add(new TermsAgreement(terms.type(), consent.version(), consent.agreed(), agreedAt));
        }
        return agreements;
    }
}
