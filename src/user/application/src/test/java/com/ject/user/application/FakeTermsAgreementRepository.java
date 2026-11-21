package com.ject.user.application;

import com.ject.user.application.port.TermsAgreementRepository;
import com.ject.user.domain.domain.TermsAgreement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class FakeTermsAgreementRepository implements TermsAgreementRepository {

    private final Map<Long, List<TermsAgreement>> byMemberId = new HashMap<>();

    @Override
    public void saveAll(Long memberId, List<TermsAgreement> agreements) {
        byMemberId.computeIfAbsent(memberId, id -> new ArrayList<>()).addAll(agreements);
    }

    List<TermsAgreement> findByMemberId(Long memberId) {
        return byMemberId.getOrDefault(memberId, List.of());
    }
}
