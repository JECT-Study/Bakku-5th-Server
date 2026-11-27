package com.ject.chat.infrastructure.adapter;

import com.ject.chat.application.dto.ChatMemberInfo;
import com.ject.chat.application.dto.ChatMemberRestriction;
import com.ject.chat.application.port.ChatMemberPort;
import com.ject.user.application.dto.MemberSummary;
import com.ject.user.application.query.BlockRelationQuery;
import com.ject.user.application.query.MemberRestrictionQuery;
import com.ject.user.application.query.MemberSummaryQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * user BC의 회원 요약 · 차단 · 이용 제한 계약을 chat 상세 조회에 필요한 형태로 조회한다
 */
@Component
@RequiredArgsConstructor
public class ChatMemberAdapter implements ChatMemberPort {

    private final MemberSummaryQuery memberSummaryQuery;
    private final BlockRelationQuery blockRelationQuery;
    private final MemberRestrictionQuery memberRestrictionQuery;

    @Override
    public ChatMemberInfo getSummary(Long memberId) {
        MemberSummary summary = memberSummaryQuery.getSummaries(List.of(memberId)).stream()
                .findFirst()
                .orElseThrow();
        return new ChatMemberInfo(summary.userId(), summary.nickname(), summary.completedCount(), summary.withdrawn());
    }

    @Override
    public boolean isBlocked(Long memberId, Long counterpartId) {
        return blockRelationQuery.isBlocked(memberId, counterpartId);
    }

    @Override
    public ChatMemberRestriction getRestriction(Long memberId) {
        return new ChatMemberRestriction(memberRestrictionQuery.getRestriction(memberId).restricted());
    }
}
