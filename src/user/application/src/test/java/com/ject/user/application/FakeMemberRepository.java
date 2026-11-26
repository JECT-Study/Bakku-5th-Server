package com.ject.user.application;

import com.ject.user.application.port.MemberRepository;
import com.ject.user.domain.domain.MannerScore;
import com.ject.user.domain.domain.Member;
import com.ject.user.domain.domain.Nickname;
import com.ject.user.domain.domain.SocialProvider;
import com.ject.user.domain.exception.DuplicateNicknameException;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 테스트용 가짜 회원 저장소. 출력 포트가 인터페이스라서 DB 없이 서비스 로직을 검증할 수 있다.
 */
class FakeMemberRepository implements MemberRepository {

    private final Map<Long, Member> store = new HashMap<>();
    private long sequence = 0;

    @Override
    public Member save(Member member) {
        Long id = member.getId() != null ? member.getId() : ++sequence;
        Member saved = copy(member, id, member.getCompletedCount());
        store.put(id, saved);
        return saved;
    }

    @Override
    public Optional<Member> findById(Long id) {
        // 실제 DB처럼 저장된 값의 복사본을 준다. 저장 전에 도메인 객체를 바꿔도 저장소 값은 그대로다
        return Optional.ofNullable(store.get(id)).map(member -> copy(member, id, member.getCompletedCount()));
    }

    @Override
    public Optional<Member> findByNickname(Nickname nickname) {
        return store.values().stream()
                .filter(member -> member.getNickname() != null)
                .filter(member -> member.getNickname().normalized().equals(nickname.normalized()))
                .findFirst();
    }

    @Override
    public Optional<Member> findActiveBySocialIdHash(SocialProvider provider, String socialIdHash) {
        return store.values().stream()
                .filter(member -> member.getProvider() == provider && socialIdHash.equals(member.getSocialIdHash()))
                .filter(member -> !member.isWithdrawn())
                .findFirst();
    }

    @Override
    public Optional<Member> findLatestWithdrawnBySocialIdHash(SocialProvider provider, String socialIdHash) {
        return store.values().stream()
                .filter(member -> member.getProvider() == provider && socialIdHash.equals(member.getSocialIdHash()))
                .filter(Member::isWithdrawn)
                .max(Comparator.comparing(Member::getWithdrawnAt));
    }

    @Override
    public List<Member> findAllByIds(Collection<Long> ids) {
        return ids.stream().map(store::get).filter(Objects::nonNull).toList();
    }

    @Override
    public void increaseCompletedCount(Collection<Long> ids, int mannerPoints) {
        ids.forEach(id -> {
            Member member = store.get(id);
            if (member != null) {
                store.put(id, copy(member, id, member.getCompletedCount() + 1,
                        new MannerScore(member.getMannerScore().value() + mannerPoints)));
            }
        });
    }

    /**
     * 실제 저장소처럼 조건(마지막 변경이 changedBefore 이전)이 맞을 때만 바꾸고, 다른 회원과 겹치면 고유 제약처럼 막는다.
     */
    @Override
    public boolean changeNickname(Long id, Nickname nickname, Instant changedAt, Instant changedBefore) {
        Member member = store.get(id);
        if (member == null) {
            return false;
        }
        boolean takenByOther = store.values().stream()
                .anyMatch(other -> !other.getId().equals(id) && other.getNickname() != null
                        && other.getNickname().normalized().equals(nickname.normalized()));
        if (takenByOther) {
            throw new DuplicateNicknameException();
        }
        Instant lastChangedAt = member.getNicknameChangedAt();
        if (lastChangedAt != null && lastChangedAt.isAfter(changedBefore)) {
            return false;
        }
        store.put(id, Member.restore(id, member.getProvider(), member.getSocialIdHash(), nickname,
                member.getBirthDate(), member.getGender(), member.getStatus(), member.getRestrictedUntil(),
                member.getCompletedCount(), member.getMannerScore(), member.isMarketingAgreed(),
                member.getMarketingChangedAt(), changedAt,
                member.getPreviousMemberId(), member.getCreatedAt(), member.getWithdrawnAt()));
        return true;
    }

    private static Member copy(Member member, Long id, int completedCount) {
        return copy(member, id, completedCount, member.getMannerScore());
    }

    private static Member copy(Member member, Long id, int completedCount, MannerScore mannerScore) {
        return Member.restore(id, member.getProvider(), member.getSocialIdHash(), member.getNickname(),
                member.getBirthDate(), member.getGender(), member.getStatus(), member.getRestrictedUntil(),
                completedCount, mannerScore, member.isMarketingAgreed(), member.getMarketingChangedAt(),
                member.getNicknameChangedAt(), member.getPreviousMemberId(), member.getCreatedAt(),
                member.getWithdrawnAt());
    }
}
