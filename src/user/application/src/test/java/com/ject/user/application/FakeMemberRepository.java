package com.ject.user.application;

import com.ject.user.application.port.MemberRepository;
import com.ject.user.domain.domain.Member;

import java.util.Collection;
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
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Member> findAllByIds(Collection<Long> ids) {
        return ids.stream().map(store::get).filter(Objects::nonNull).toList();
    }

    @Override
    public void increaseCompletedCount(Collection<Long> ids) {
        ids.forEach(id -> {
            Member member = store.get(id);
            if (member != null) {
                store.put(id, copy(member, id, member.getCompletedCount() + 1));
            }
        });
    }

    private static Member copy(Member member, Long id, int completedCount) {
        return Member.restore(id, member.getProvider(), member.getSocialIdHash(), member.getNickname(),
                member.getBirthDate(), member.getGender(), member.getStatus(), member.getRestrictedUntil(),
                completedCount, member.isMarketingAgreed(), member.getMarketingChangedAt(),
                member.getNicknameChangedAt(), member.getPreviousMemberId(), member.getCreatedAt(),
                member.getWithdrawnAt());
    }
}
