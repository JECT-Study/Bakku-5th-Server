package com.ject.exchange.application.service.contract;

import com.ject.exchange.domain.domain.exchange.Exchange;
import com.ject.exchange.domain.domain.exchange.ExchangeStatus;
import com.ject.exchange.domain.repository.ExchangeRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 테스트용 가짜 교환 저장소. 참여자 확인 계약은 조회만 하므로 미리 넣은 교환을 그대로 돌려준다.
 */
class FakeExchangeRepository implements ExchangeRepository {

    private final Map<Long, Exchange> store = new HashMap<>();

    @Override
    public Exchange save(Exchange exchange) {
        throw new IllegalStateException("참여자 확인은 교환을 저장하지 않는다");
    }

    @Override
    public Optional<Exchange> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<Exchange> findByIdForUpdate(Long id) {
        return findById(id);
    }

    /** 교환이 요청한 글 ID만 돌려준다. 교환이 없으면 빈 값이다. */
    @Override
    public Optional<Long> findPostIdOf(Long id) {
        return Optional.ofNullable(store.get(id)).map(Exchange::getPostId);
    }

    @Override
    public boolean existsCompletedByPostIdExcludingExchange(Long postId, Long excludedExchangeId) {
        return store.values().stream().anyMatch(exchange -> exchange.getPostId().equals(postId)
                && !exchange.getId().equals(excludedExchangeId)
                && exchange.getStatus() == ExchangeStatus.COMPLETED);
    }

    void preload(Exchange exchange) {
        store.put(exchange.getId(), exchange);
    }
}
