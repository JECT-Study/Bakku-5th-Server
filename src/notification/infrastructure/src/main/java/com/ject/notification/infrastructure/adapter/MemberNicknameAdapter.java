package com.ject.notification.infrastructure.adapter;

import com.ject.notification.application.port.MemberNicknamePort;
import com.ject.user.application.query.MemberSummaryQuery;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MemberNicknameAdapter implements MemberNicknamePort {

    private final MemberSummaryQuery memberSummaryQuery;

    public MemberNicknameAdapter(MemberSummaryQuery memberSummaryQuery) {
        this.memberSummaryQuery = memberSummaryQuery;
    }

    @Override
    public String getNickname(Long memberId) {
        return memberSummaryQuery.getSummaries(List.of(memberId)).getFirst().nickname();
    }
}
