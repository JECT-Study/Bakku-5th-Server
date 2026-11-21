package com.ject.exchange.infrastructure.adapter;

import com.ject.exchange.application.dto.dto.UserInfo;
import com.ject.exchange.application.port.UserPort;
import com.ject.common.core.exception.DomainException;
import com.ject.user.application.dto.MemberSummary;
import com.ject.user.application.query.MemberSummaryQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 교환 참여자 정보를 user BC의 회원 요약 계약으로 조회한다 (결정 B8, post AuthorAdapter와 같은 방식)
 */
@Component
@RequiredArgsConstructor
public class UserAdapter implements UserPort {

    private final MemberSummaryQuery memberSummaryQuery;

    /**
     * 호출한 쪽의 트랜잭션 밖에서 조회한다.
     * 회원 요약 서비스는 읽기 트랜잭션에 참여하므로, 없는 회원 예외가 나면 호출한 쪽 트랜잭션까지 rollback-only가 되어
     * 예외를 잡아도 커밋 시점에 실패한다. 트랜잭션을 분리해 실패한 조회만 롤백되게 한다
     */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public List<UserInfo> findAllByIds(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        return summariesOf(userIds).stream()
                .map(UserAdapter::toUserInfo)
                .toList();
    }

    /**
     * 회원 요약 계약은 없는 ID가 하나라도 있으면 전체를 예외로 끝낸다.
     * 글 정보가 아직 stub이라 게시자가 실제 회원이 아닐 수 있으므로, 실패하면 ID별로 다시 조회해 없는 회원만 뺀다
     */
    private List<MemberSummary> summariesOf(Collection<Long> userIds) {
        try {
            return memberSummaryQuery.getSummaries(userIds);
        } catch (DomainException e) {
            return userIds.stream()
                    .map(this::summaryOf)
                    .flatMap(Optional::stream)
                    .toList();
        }
    }

    private Optional<MemberSummary> summaryOf(Long userId) {
        try {
            return memberSummaryQuery.getSummaries(List.of(userId)).stream().findFirst();
        } catch (DomainException e) {
            return Optional.empty();
        }
    }

    private static UserInfo toUserInfo(MemberSummary summary) {
        return new UserInfo(summary.userId(), summary.nickname(), summary.completedCount());
    }
}
