package com.ject.exchange.infrastructure.adapter;

import com.ject.user.application.dto.MemberSummary;
import com.ject.user.application.query.MemberSummaryQuery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 테스트용 가짜 회원 요약 조회. user BC 계약대로 없는 id가 하나라도 있으면 전체 호출이 예외로 끝난다.
 */
class FakeMemberSummaryQuery implements MemberSummaryQuery {

    private final Map<Long, MemberSummary> summaries = new HashMap<>();
    private final List<List<Long>> calls = new ArrayList<>();

    @Override
    public List<MemberSummary> getSummaries(Collection<Long> memberIds) {
        calls.add(List.copyOf(memberIds));
        boolean anyMissing = memberIds.stream().anyMatch(id -> !summaries.containsKey(id));
        if (anyMissing) {
            throw new TestMemberNotFoundException();
        }
        return memberIds.stream().map(summaries::get).toList();
    }

    void register(MemberSummary summary) {
        summaries.put(summary.userId(), summary);
    }

    List<List<Long>> calls() {
        return calls;
    }
}
