package com.ject.user.infrastructure.persistence.mapper;

import com.ject.user.application.port.MemberRepository;
import com.ject.user.domain.domain.Member;
import com.ject.user.domain.domain.MemberStatus;
import com.ject.user.domain.domain.Nickname;
import com.ject.user.domain.domain.SocialProvider;
import com.ject.user.domain.exception.AlreadySignedUpException;
import com.ject.user.domain.exception.DuplicateNicknameException;
import com.ject.user.domain.exception.MemberNotFoundException;
import com.ject.user.infrastructure.persistence.entity.MemberJpaEntity;
import com.ject.user.infrastructure.persistence.repository.MemberJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * application의 회원 저장소 포트를 JPA로 구현한 어댑터.
 */
@Repository
public class MemberRepositoryAdapter implements MemberRepository {

    private static final String NICKNAME_UNIQUE_INDEX = "uk_user_member_nickname";
    private static final String SOCIAL_ACCOUNT_UNIQUE_INDEX = "uk_user_member_provider_social_id_hash";

    private final MemberJpaRepository memberJpaRepository;
    private final Clock clock;

    public MemberRepositoryAdapter(MemberJpaRepository memberJpaRepository, Clock clock) {
        this.memberJpaRepository = memberJpaRepository;
        this.clock = clock;
    }

    /**
     * 새 회원은 추가하고, 저장된 회원은 불러온 엔티티의 값을 바꾼다.
     */
    @Override
    public Member save(Member member) {
        if (member.getId() == null) {
            return MemberMapper.toDomain(insert(member));
        }
        MemberJpaEntity entity = memberJpaRepository.findById(member.getId())
                .orElseThrow(() -> new MemberNotFoundException(member.getId()));
        MemberMapper.apply(member, entity);
        return MemberMapper.toDomain(memberJpaRepository.save(entity));
    }

    /**
     * 확인 뒤 같은 닉네임 · 같은 소셜 계정으로 동시에 가입하면 DB 고유 제약에서 걸린다 (결정 C8).
     * id가 IDENTITY라 insert가 바로 실행되므로 여기서 제약 이름으로 구분해 비즈니스 예외로 바꾼다.
     */
    private MemberJpaEntity insert(Member member) {
        try {
            return memberJpaRepository.save(MemberMapper.toNewEntity(member));
        } catch (DataIntegrityViolationException e) {
            String message = String.valueOf(e.getMostSpecificCause().getMessage());
            if (message.contains(NICKNAME_UNIQUE_INDEX)) {
                throw new DuplicateNicknameException();
            }
            if (message.contains(SOCIAL_ACCOUNT_UNIQUE_INDEX)) {
                throw new AlreadySignedUpException();
            }
            throw e;
        }
    }

    @Override
    public Optional<Member> findById(Long id) {
        return memberJpaRepository.findById(id).map(MemberMapper::toDomain);
    }

    @Override
    public Optional<Member> findByNickname(Nickname nickname) {
        return memberJpaRepository.findByNicknameIgnoreCase(nickname.value()).map(MemberMapper::toDomain);
    }

    @Override
    public Optional<Member> findActiveBySocialIdHash(SocialProvider provider, String socialIdHash) {
        return memberJpaRepository.findActiveBySocialIdHash(provider, socialIdHash).map(MemberMapper::toDomain);
    }

    @Override
    public Optional<Member> findLatestWithdrawnBySocialIdHash(SocialProvider provider, String socialIdHash) {
        return memberJpaRepository.findFirstByProviderAndSocialIdHashAndStatusOrderByWithdrawnAtDesc(
                provider, socialIdHash, MemberStatus.WITHDRAWN).map(MemberMapper::toDomain);
    }

    @Override
    public List<Member> findAllByIds(Collection<Long> ids) {
        return memberJpaRepository.findAllById(ids).stream()
                .map(MemberMapper::toDomain)
                .toList();
    }

    @Override
    public void increaseCompletedCount(Collection<Long> ids) {
        memberJpaRepository.increaseCompletedCount(ids, clock.instant());
    }

    /**
     * 확인 뒤 다른 회원이 같은 닉네임을 먼저 쓰면 lower(nickname) 고유 인덱스에서 걸린다 (결정 C8).
     */
    @Override
    public boolean changeNickname(Long id, Nickname nickname, Instant changedAt, Instant changedBefore) {
        try {
            return memberJpaRepository.changeNicknameIfChangeable(id, nickname.value(), changedAt, changedBefore) > 0;
        } catch (DataIntegrityViolationException e) {
            if (String.valueOf(e.getMostSpecificCause().getMessage()).contains(NICKNAME_UNIQUE_INDEX)) {
                throw new DuplicateNicknameException();
            }
            throw e;
        }
    }
}
