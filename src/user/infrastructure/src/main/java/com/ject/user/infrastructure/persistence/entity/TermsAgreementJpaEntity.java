package com.ject.user.infrastructure.persistence.entity;

import com.ject.common.jpa.BaseEntity;
import com.ject.user.domain.domain.TermsType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "user_terms_agreement")
public class TermsAgreementJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false, updatable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "terms_type", nullable = false, updatable = false, length = 30)
    private TermsType termsType;

    @Column(name = "terms_version", nullable = false, updatable = false, length = 20)
    private String termsVersion;

    @Column(nullable = false, updatable = false)
    private boolean agreed;

    @Column(name = "agreed_at", nullable = false, updatable = false)
    private Instant agreedAt;

    protected TermsAgreementJpaEntity() {
    }

    public TermsAgreementJpaEntity(Long memberId, TermsType termsType, String termsVersion, boolean agreed,
                                   Instant agreedAt) {
        this.memberId = memberId;
        this.termsType = termsType;
        this.termsVersion = termsVersion;
        this.agreed = agreed;
        this.agreedAt = agreedAt;
    }
}
