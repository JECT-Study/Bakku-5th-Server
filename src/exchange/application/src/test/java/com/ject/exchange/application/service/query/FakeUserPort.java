package com.ject.exchange.application.service.query;

import com.ject.exchange.application.dto.dto.UserInfo;
import com.ject.exchange.application.port.UserPort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 테스트용 가짜 회원 Port. UserPort 계약대로 없는 회원 id는 결과에서 빠지고 예외를 던지지 않는다.
 */
class FakeUserPort implements UserPort {

    private final Map<Long, UserInfo> users = new HashMap<>();
    private final Set<List<Long>> blocks = new HashSet<>();
    private final List<List<Long>> requestedIds = new ArrayList<>();

    @Override
    public List<UserInfo> findAllByIds(List<Long> userIds) {
        requestedIds.add(List.copyOf(userIds));
        return userIds.stream()
                .map(users::get)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public boolean isBlocked(Long memberId, Long otherMemberId) {
        return blocks.contains(List.of(memberId, otherMemberId))
                || blocks.contains(List.of(otherMemberId, memberId));
    }

    /** blocker가 blocked를 차단한 관계를 등록한다. 판정은 양방향이다. */
    void block(Long blockerId, Long blockedId) {
        blocks.add(List.of(blockerId, blockedId));
    }

    void unblock(Long blockerId, Long blockedId) {
        blocks.remove(List.of(blockerId, blockedId));
    }

    void register(UserInfo user) {
        users.put(user.id(), user);
    }

    List<List<Long>> requestedIds() {
        return requestedIds;
    }
}
