package com.ject.user.domain.domain;

import com.ject.user.domain.exception.MemberAlreadyWithdrawnException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;

/**
 * 회원 Aggregate Root. 다른 BC는 회원을 id(FK)로만 참조한다.
 * 탈퇴해도 행은 남기고 개인정보만 비운다 (결정 A4-2). 소셜 식별값은 HMAC 해시로만 갖는다 (결정 A4-1 · C9).
 */
public class Member {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final Long id;
    private final SocialProvider provider;
    private final String socialIdHash;
    private Nickname nickname;
    private BirthDate birthDate;
    private Gender gender;
    private MemberStatus status;
    private final Instant restrictedUntil;
    private int completedCount;
    private final boolean marketingAgreed;
    private final Instant marketingChangedAt;
    private final Instant nicknameChangedAt;
    private final Long previousMemberId;
    private final Instant createdAt;
    private Instant withdrawnAt;

    private Member(Long id, SocialProvider provider, String socialIdHash, Nickname nickname, BirthDate birthDate,
                   Gender gender, MemberStatus status, Instant restrictedUntil, int completedCount,
                   boolean marketingAgreed, Instant marketingChangedAt, Instant nicknameChangedAt,
                   Long previousMemberId, Instant createdAt, Instant withdrawnAt) {
        this.id = id;
        this.provider = Objects.requireNonNull(provider);
        this.socialIdHash = Objects.requireNonNull(socialIdHash);
        this.nickname = nickname;
        this.birthDate = birthDate;
        this.gender = gender;
        this.status = Objects.requireNonNull(status);
        this.restrictedUntil = restrictedUntil;
        this.completedCount = completedCount;
        this.marketingAgreed = marketingAgreed;
        this.marketingChangedAt = marketingChangedAt;
        this.nicknameChangedAt = nicknameChangedAt;
        this.previousMemberId = previousMemberId;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.withdrawnAt = withdrawnAt;
    }

    /**
     * 가입. 만 14세 미만이면 가입할 수 없다 (A-04-1).
     * 가입 때 정한 닉네임은 변경 횟수에 넣지 않으므로 nicknameChangedAt은 비워 둔다 (결정 D2).
     *
     * @param gender           선택 입력, null 가능
     * @param previousMemberId 탈퇴 후 재가입이면 이전 회원 id, 아니면 null (결정 A4-3)
     */
    public static Member create(SocialProvider provider, String socialIdHash, Nickname nickname, BirthDate birthDate,
                                Gender gender, boolean marketingAgreed, Long previousMemberId, Instant now) {
        Objects.requireNonNull(nickname);
        Objects.requireNonNull(birthDate);
        birthDate.ensureSignupAge(LocalDate.ofInstant(now, KST));
        return new Member(null, provider, socialIdHash, nickname, birthDate, gender, MemberStatus.ACTIVE, null, 0,
                marketingAgreed, now, null, previousMemberId, now, null);
    }

    // 저장소에서 읽어온 데이터로 복원할 때 사용
    public static Member restore(Long id, SocialProvider provider, String socialIdHash, Nickname nickname,
                                 BirthDate birthDate, Gender gender, MemberStatus status, Instant restrictedUntil,
                                 int completedCount, boolean marketingAgreed, Instant marketingChangedAt,
                                 Instant nicknameChangedAt, Long previousMemberId, Instant createdAt,
                                 Instant withdrawnAt) {
        return new Member(id, provider, socialIdHash, nickname, birthDate, gender, status, restrictedUntil,
                completedCount, marketingAgreed, marketingChangedAt, nicknameChangedAt, previousMemberId, createdAt,
                withdrawnAt);
    }

    /**
     * 탈퇴. 닉네임 · 생년월일 · 성별은 즉시 비우고, 소셜 식별값 해시는 재가입 확인용으로 남긴다 (G-09-2, 결정 A4-2).
     * 진행 중인 교환이 있어도 탈퇴할 수 있다 (결정 B5).
     */
    public void withdraw(Instant now) {
        if (isWithdrawn()) {
            throw new MemberAlreadyWithdrawnException(id);
        }
        this.nickname = null;
        this.birthDate = null;
        this.gender = null;
        this.status = MemberStatus.WITHDRAWN;
        this.withdrawnAt = now;
    }

    /**
     * 교환 완료 횟수 +1. 자동 완료된 교환도 포함한다 (F-01-2, G-04-1, 결정 B7).
     */
    public void increaseCompletedCount() {
        this.completedCount++;
    }

    public boolean isWithdrawn() {
        return status == MemberStatus.WITHDRAWN;
    }

    public boolean isBanned() {
        return status == MemberStatus.BANNED;
    }

    /**
     * 상대 화면에 탈퇴한 회원으로 보이는지. 영구 정지 회원도 탈퇴와 같게 보인다 (명세 MemberStatus.BANNED).
     */
    public boolean appearsWithdrawn() {
        return isWithdrawn() || isBanned();
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

    /** 탈퇴하면 null */
    public Nickname getNickname() {
        return nickname;
    }

    /** 탈퇴하면 null */
    public BirthDate getBirthDate() {
        return birthDate;
    }

    /** 선택 입력이라 null 가능, 탈퇴하면 null */
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getWithdrawnAt() {
        return withdrawnAt;
    }
}
