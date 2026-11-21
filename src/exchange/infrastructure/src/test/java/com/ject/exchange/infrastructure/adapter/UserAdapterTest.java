package com.ject.exchange.infrastructure.adapter;

import com.ject.exchange.application.dto.dto.UserInfo;
import com.ject.user.application.dto.MemberSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 교환 참여자 정보를 user BC 회원 요약 계약(MemberSummaryQuery)으로 채우는지 확인한다.
 */
class UserAdapterTest {

    private static final Long MEMBER_A = 101L;
    private static final Long MEMBER_B = 102L;
    private static final Long MISSING_MEMBER = 100L;
    private static final Long ANOTHER_MISSING_MEMBER = 200L;

    private FakeMemberSummaryQuery memberSummaryQuery;
    private UserAdapter userAdapter;

    @BeforeEach
    void setUp() {
        memberSummaryQuery = new FakeMemberSummaryQuery();
        userAdapter = new UserAdapter(memberSummaryQuery);
    }

    @Test
    @DisplayName("회원 요약의 id · 닉네임 · 교환 완료 횟수를 UserInfo로 바꾼다")
    void findAllByIds_mapsMemberSummaries() {
        // given
        memberSummaryQuery.register(new MemberSummary(MEMBER_A, "바꾸", 3, false));
        memberSummaryQuery.register(new MemberSummary(MEMBER_B, "하치", 0, false));

        // when
        Map<Long, UserInfo> users = byId(userAdapter.findAllByIds(List.of(MEMBER_A, MEMBER_B)));

        // then
        assertEquals(2, users.size());
        assertEquals(new UserInfo(MEMBER_A, "바꾸", 3), users.get(MEMBER_A));
        assertEquals(new UserInfo(MEMBER_B, "하치", 0), users.get(MEMBER_B));
    }

    @Test
    @DisplayName("모든 회원이 있으면 회원 요약을 한 번만 조회한다")
    void findAllByIds_allPresent_callsOnce() {
        memberSummaryQuery.register(new MemberSummary(MEMBER_A, "바꾸", 3, false));
        memberSummaryQuery.register(new MemberSummary(MEMBER_B, "하치", 0, false));

        userAdapter.findAllByIds(List.of(MEMBER_A, MEMBER_B));

        assertEquals(1, memberSummaryQuery.calls().size());
    }

    @Test
    @DisplayName("탈퇴 회원은 id와 완료 횟수를 유지하고 닉네임은 탈퇴한 사용자다")
    void findAllByIds_withdrawnMember_keepsIdAndCount() {
        memberSummaryQuery.register(new MemberSummary(MEMBER_A, "바꾸", 3, false));
        memberSummaryQuery.register(new MemberSummary(MEMBER_B, MemberSummary.WITHDRAWN_NICKNAME, 5, true));

        Map<Long, UserInfo> users = byId(userAdapter.findAllByIds(List.of(MEMBER_A, MEMBER_B)));

        assertEquals(new UserInfo(MEMBER_B, "탈퇴한 사용자", 5), users.get(MEMBER_B));
        assertEquals(new UserInfo(MEMBER_A, "바꾸", 3), users.get(MEMBER_A));
    }

    @Test
    @DisplayName("없는 회원이 섞여 있으면 예외 없이 그 회원만 빼고 돌려준다")
    void findAllByIds_someMissing_excludesMissing() {
        memberSummaryQuery.register(new MemberSummary(MEMBER_A, "바꾸", 3, false));

        List<UserInfo> users = userAdapter.findAllByIds(List.of(MEMBER_A, MISSING_MEMBER));

        assertEquals(List.of(new UserInfo(MEMBER_A, "바꾸", 3)), users);
    }

    @Test
    @DisplayName("없는 회원이 섞이면 한 번에 조회한 뒤 id마다 다시 조회해 최대 3회 호출한다")
    void findAllByIds_someMissing_retriesPerId() {
        memberSummaryQuery.register(new MemberSummary(MEMBER_A, "바꾸", 3, false));

        userAdapter.findAllByIds(List.of(MEMBER_A, MISSING_MEMBER));

        List<List<Long>> calls = memberSummaryQuery.calls();
        assertEquals(3, calls.size());
        assertEquals(2, calls.get(0).size());
        assertTrue(calls.subList(1, 3).stream().allMatch(call -> call.size() == 1));
    }

    @Test
    @DisplayName("모든 회원이 없으면 예외 없이 빈 목록을 돌려준다")
    void findAllByIds_allMissing_returnsEmpty() {
        List<UserInfo> users = userAdapter.findAllByIds(List.of(MISSING_MEMBER, ANOTHER_MISSING_MEMBER));

        assertTrue(users.isEmpty());
    }

    private static Map<Long, UserInfo> byId(List<UserInfo> users) {
        return users.stream().collect(Collectors.toMap(UserInfo::id, Function.identity()));
    }
}
