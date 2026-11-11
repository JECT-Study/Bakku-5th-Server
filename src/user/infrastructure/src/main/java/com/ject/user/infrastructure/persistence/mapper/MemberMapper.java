package com.ject.user.infrastructure.persistence.mapper;

import com.ject.user.domain.domain.BirthDate;
import com.ject.user.domain.domain.Member;
import com.ject.user.domain.domain.Nickname;
import com.ject.user.infrastructure.persistence.entity.MemberJpaEntity;

import java.time.LocalDate;

/**
 * 도메인 회원 ↔ JPA 엔티티 변환. 탈퇴하면 닉네임 · 생년월일이 비어 있으므로 null을 그대로 옮긴다.
 */
final class MemberMapper {

    private MemberMapper() {
    }

    static MemberJpaEntity toNewEntity(Member member) {
        return new MemberJpaEntity(
                member.getProvider(),
                member.getSocialIdHash(),
                nicknameOf(member),
                birthDateOf(member),
                member.getGender(),
                member.getStatus(),
                member.getRestrictedUntil(),
                member.getCompletedCount(),
                member.isMarketingAgreed(),
                member.getMarketingChangedAt(),
                member.getNicknameChangedAt(),
                member.getPreviousMemberId(),
                member.getWithdrawnAt()
        );
    }

    static void apply(Member member, MemberJpaEntity entity) {
        entity.update(
                member.getSocialIdHash(),
                nicknameOf(member),
                birthDateOf(member),
                member.getGender(),
                member.getStatus(),
                member.getRestrictedUntil(),
                member.getCompletedCount(),
                member.isMarketingAgreed(),
                member.getMarketingChangedAt(),
                member.getNicknameChangedAt(),
                member.getWithdrawnAt()
        );
    }

    static Member toDomain(MemberJpaEntity entity) {
        return Member.restore(
                entity.getId(),
                entity.getProvider(),
                entity.getSocialIdHash(),
                entity.getNickname() == null ? null : new Nickname(entity.getNickname()),
                entity.getBirthDate() == null ? null : new BirthDate(entity.getBirthDate()),
                entity.getGender(),
                entity.getStatus(),
                entity.getRestrictedUntil(),
                entity.getCompletedCount(),
                entity.isMarketingAgreed(),
                entity.getMarketingChangedAt(),
                entity.getNicknameChangedAt(),
                entity.getPreviousMemberId(),
                entity.getCreatedAt(),
                entity.getWithdrawnAt()
        );
    }

    private static String nicknameOf(Member member) {
        return member.getNickname() == null ? null : member.getNickname().value();
    }

    private static LocalDate birthDateOf(Member member) {
        return member.getBirthDate() == null ? null : member.getBirthDate().value();
    }
}
