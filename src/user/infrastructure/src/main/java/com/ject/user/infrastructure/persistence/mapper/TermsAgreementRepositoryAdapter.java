package com.ject.user.infrastructure.persistence.mapper;

import com.ject.user.application.port.TermsAgreementRepository;
import com.ject.user.domain.domain.TermsAgreement;
import com.ject.user.infrastructure.persistence.entity.TermsAgreementJpaEntity;
import com.ject.user.infrastructure.persistence.repository.TermsAgreementJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TermsAgreementRepositoryAdapter implements TermsAgreementRepository {

    private final TermsAgreementJpaRepository termsAgreementJpaRepository;

    public TermsAgreementRepositoryAdapter(TermsAgreementJpaRepository termsAgreementJpaRepository) {
        this.termsAgreementJpaRepository = termsAgreementJpaRepository;
    }

    @Override
    public void saveAll(Long memberId, List<TermsAgreement> agreements) {
        termsAgreementJpaRepository.saveAll(agreements.stream()
                .map(agreement -> new TermsAgreementJpaEntity(memberId, agreement.type(), agreement.version(),
                        agreement.agreed(), agreement.agreedAt()))
                .toList());
    }
}
