package com.ject.exchange.domain.repository;

import com.ject.exchange.domain.domain.exchange.Exchange;

import java.util.Optional;

public interface ExchangeRepository {
    Exchange save(Exchange exchange);
    Optional<Exchange> findById(Long id);

    /**
     * 같은 교환에서 동시에 들어온 쓰기 요청을 차례로 처리하기 위해 교환을 잠그고 조회한다
     */
    Optional<Exchange> findByIdForUpdate(Long id);
}
