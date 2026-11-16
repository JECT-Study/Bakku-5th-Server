package com.ject.user.infrastructure.persistence.mapper;

import com.ject.user.application.port.MemberRepository;
import com.ject.user.domain.domain.Member;
import com.ject.user.domain.domain.Nickname;
import com.ject.user.domain.exception.MemberNotFoundException;
import com.ject.user.infrastructure.persistence.entity.MemberJpaEntity;
import com.ject.user.infrastructure.persistence.repository.MemberJpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * application의 회원 저장소 포트를 JPA로 구현한 어댑터.
 */
@Repository
public class MemberRepositoryAdapter implements MemberRepository {

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
            MemberJpaEntity saved = memberJpaRepository.save(MemberMapper.toNewEntity(member));
            return MemberMapper.toDomain(saved);
        }
        MemberJpaEntity entity = memberJpaRepository.findById(member.getId())
                .orElseThrow(() -> new MemberNotFoundException(member.getId()));
        MemberMapper.apply(member, entity);
        return MemberMapper.toDomain(memberJpaRepository.save(entity));
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
    public List<Member> findAllByIds(Collection<Long> ids) {
        return memberJpaRepository.findAllById(ids).stream()
                .map(MemberMapper::toDomain)
                .toList();
    }

    @Override
    public void increaseCompletedCount(Collection<Long> ids) {
        memberJpaRepository.increaseCompletedCount(ids, clock.instant());
    }
}
