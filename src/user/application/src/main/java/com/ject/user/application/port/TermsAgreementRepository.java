package com.ject.user.application.port;

import com.ject.user.domain.domain.TermsAgreement;

import java.util.List;

/**
 * 약관 동의 이력 저장소 출력 포트.
 */
public interface TermsAgreementRepository {

    void saveAll(Long memberId, List<TermsAgreement> agreements);
}
