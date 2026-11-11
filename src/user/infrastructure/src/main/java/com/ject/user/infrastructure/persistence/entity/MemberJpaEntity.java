package com.ject.user.infrastructure.persistence.entity;

import com.ject.common.jpa.BaseEntity;
import com.ject.user.domain.domain.Gender;
import com.ject.user.domain.domain.MemberStatus;
import com.ject.user.domain.domain.SocialProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "user_member")
public class MemberJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 10)
    private SocialProvider provider;

    @Column(name = "social_id_hash", length = 64)
    private String socialIdHash;

    @Column(length = 10)
    private String nickname;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status;

    @Column(name = "restricted_until")
    private Instant restrictedUntil;

    @Column(name = "completed_count", nullable = false)
    private int completedCount;

    @Column(name = "marketing_agreed", nullable = false)
    private boolean marketingAgreed;

    @Column(name = "marketing_changed_at", nullable = false)
    private Instant marketingChangedAt;

    @Column(name = "nickname_changed_at")
    private Instant nicknameChangedAt;

    @Column(name = "previous_member_id", updatable = false)
    private Long previousMemberId;

    @Column(name = "withdrawn_at")
    private Instant withdrawnAt;

    protected MemberJpaEntity() {
    }

    public MemberJpaEntity(SocialProvider provider, String socialIdHash, String nickname, LocalDate birthDate,
                           Gender gender, MemberStatus status, Instant restrictedUntil, int completedCount,
                           boolean marketingAgreed, Instant marketingChangedAt, Instant nicknameChangedAt,
                           Long previousMemberId, Instant withdrawnAt) {
        this.provider = provider;
        this.previousMemberId = previousMemberId;
        update(socialIdHash, nickname, birthDate, gender, status, restrictedUntil, completedCount, marketingAgreed,
                marketingChangedAt, nicknameChangedAt, withdrawnAt);
    }

    /**
     * 이미 저장된 회원의 값을 바꾼다. 새 엔티티로 병합하면 생성 시각이 비므로 불러온 엔티티를 고친다.
     */
    public void update(String socialIdHash, String nickname, LocalDate birthDate, Gender gender, MemberStatus status,
                       Instant restrictedUntil, int completedCount, boolean marketingAgreed,
                       Instant marketingChangedAt, Instant nicknameChangedAt, Instant withdrawnAt) {
        this.socialIdHash = socialIdHash;
        this.nickname = nickname;
        this.birthDate = birthDate;
        this.gender = gender;
        this.status = status;
        this.restrictedUntil = restrictedUntil;
        this.completedCount = completedCount;
        this.marketingAgreed = marketingAgreed;
        this.marketingChangedAt = marketingChangedAt;
        this.nicknameChangedAt = nicknameChangedAt;
        this.withdrawnAt = withdrawnAt;
    }

    public Long getId() {
        return id;
    }

    public SocialProvider getProvider() {
        return provider;
    }

    public String getSocialIdHash() {
        return socialIdHash;
    }

    public String getNickname() {
        return nickname;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public Gender getGender() {
        return gender;
    }

    public MemberStatus getStatus() {
        return status;
    }

    public Instant getRestrictedUntil() {
        return restrictedUntil;
    }

    public int getCompletedCount() {
        return completedCount;
    }

    public boolean isMarketingAgreed() {
        return marketingAgreed;
    }

    public Instant getMarketingChangedAt() {
        return marketingChangedAt;
    }

    public Instant getNicknameChangedAt() {
        return nicknameChangedAt;
    }

    public Long getPreviousMemberId() {
        return previousMemberId;
    }

    public Instant getWithdrawnAt() {
        return withdrawnAt;
    }
}
