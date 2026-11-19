package com.ject.exchange.application.service.command;

import com.ject.exchange.domain.domain.exchange.Exchange;
import com.ject.exchange.domain.repository.ExchangeRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 테스트용 가짜 교환 저장소. Handler가 무엇을 저장했는지 DB 없이 확인한다.
 * 테스트는 id가 채워진 교환만 미리 넣으므로 저장한 객체를 그대로 보관한다.
 */
class FakeExchangeRepository implements ExchangeRepository {

    private final Map<Long, Exchange> store = new HashMap<>();
    private int saveCount = 0;

    @Override
    public Exchange save(Exchange exchange) {
        if (exchange.getId() == null) {
            throw new IllegalStateException("이 테스트는 id가 있는 교환만 저장한다");
        }
        store.put(exchange.getId(), exchange);
        saveCount++;
        return exchange;
    }

    @Override
    public Optional<Exchange> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    /** 테스트 준비용 저장. 저장 횟수에 포함하지 않는다. */
    void preload(Exchange exchange) {
        store.put(exchange.getId(), exchange);
    }

    int saveCount() {
        return saveCount;
    }
}
