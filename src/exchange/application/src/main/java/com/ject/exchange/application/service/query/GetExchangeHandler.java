package com.ject.exchange.application.service.query;

import com.ject.common.core.cqrs.query.QueryHandler;
import com.ject.exchange.application.dto.dto.ExchangeResult;
import com.ject.exchange.application.dto.dto.PostInfo;
import com.ject.exchange.application.dto.dto.UserInfo;
import com.ject.exchange.application.dto.query.GetExchangeQuery;
import com.ject.exchange.application.port.PostPort;
import com.ject.exchange.application.port.UserPort;
import com.ject.exchange.domain.domain.exchange.Exchange;
import com.ject.exchange.domain.domain.exchange.OfferedItem;
import com.ject.exchange.domain.exception.ExchangeErrorCode;
import com.ject.exchange.domain.exception.ExchangeException;
import com.ject.exchange.domain.repository.ExchangeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class GetExchangeHandler implements QueryHandler<GetExchangeQuery, ExchangeResult> {

    private final ExchangeRepository exchangeRepository;
    private final PostPort postPort;
    private final UserPort userPort;

    @Override
    @Transactional(readOnly = true)
    public ExchangeResult handle(GetExchangeQuery query) {
        Exchange exchange = exchangeRepository.findById(query.exchangeId())
                .orElseThrow(() -> new ExchangeException(ExchangeErrorCode.EXCHANGE_NOT_FOUND));
        if(!exchange.isParticipant(query.viewerId())) {
            throw new ExchangeException(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT);
        }

        Map<Long, PostInfo> posts = postPort.findAllByIds(postIdsOf(exchange)).stream()
                .collect(Collectors.toMap(PostInfo::id, p -> p));
        Map<Long, UserInfo> users = userPort.findAllByIds(List.of(exchange.getRequestUserId(), exchange.getOwnerId())).stream()
                .collect(Collectors.toMap(UserInfo::id, u -> u));
        return ExchangeResult.of(exchange, posts, users);
    }

    private static List<Long> postIdsOf(Exchange exchange) {
        return Stream.concat(
                        Stream.of(exchange.getPostId()),
                        exchange.getOfferedItems().stream()
                                .map(OfferedItem::getSourcePostId)
                                .filter(Objects::nonNull))
                .distinct()
                .toList();
    }
}
