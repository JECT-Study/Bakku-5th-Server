package com.ject.user.infrastructure.persistence.repository;

import com.ject.user.infrastructure.persistence.entity.TermsAgreementJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermsAgreementJpaRepository extends JpaRepository<TermsAgreementJpaEntity, Long> {
}
