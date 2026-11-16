package com.ject.post.infrastructure.adapter;

import com.ject.post.application.dto.AuthorInfo;
import com.ject.post.application.port.AuthorPort;
import com.ject.user.application.dto.MemberSummary;
import com.ject.user.application.query.MemberSummaryQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AuthorAdapter implements AuthorPort {

    private final MemberSummaryQuery memberSummaryQuery;

    @Override
    public Map<Long, AuthorInfo> findAuthors(Collection<Long> authorIds) {
        return memberSummaryQuery.getSummaries(authorIds).stream()
                .collect(Collectors.toMap(MemberSummary::userId, AuthorAdapter::toAuthorInfo));
    }

    private static AuthorInfo toAuthorInfo(MemberSummary summary) {
        Long userId = summary.withdrawn() ? null : summary.userId();
        return new AuthorInfo(userId, summary.nickname(), summary.completedCount());
    }
}
