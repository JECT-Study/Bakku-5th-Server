package com.ject.user.application.service;

import com.ject.user.application.dto.MemberSummary;
import com.ject.user.application.port.MemberRepository;
import com.ject.user.application.query.MemberSummaryQuery;
import com.ject.user.domain.domain.Member;
import com.ject.user.domain.exception.MemberNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 다른 BC에 주는 회원 요약 조회 (결정 B8 · B16).
 */
@Service
@Transactional(readOnly = true)
public class MemberSummaryService implements MemberSummaryQuery {

    private final MemberRepository memberRepository;

    public MemberSummaryService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    public List<MemberSummary> getSummaries(Collection<Long> memberIds) {
        Set<Long> ids = Set.copyOf(memberIds);
        if (ids.isEmpty()) {
            return List.of();
        }
        List<Member> members = memberRepository.findAllByIds(ids);
        if (members.size() != ids.size()) {
            Set<Long> found = members.stream().map(Member::getId).collect(Collectors.toSet());
            Long missingId = ids.stream().filter(id -> !found.contains(id)).findFirst().orElseThrow();
            throw new MemberNotFoundException(missingId);
        }
        return members.stream().map(MemberSummary::from).toList();
    }
}
