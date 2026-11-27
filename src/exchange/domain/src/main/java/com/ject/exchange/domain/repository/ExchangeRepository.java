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

    /**
     * 교환이 요청한 글 ID만 조회한다. 글 잠금을 잡기 전에 교환을 읽지 않으려고 쓴다 (잠금 뒤 처음 읽어야 최신 상태가 보인다).
     * 구현은 교환을 영속성 컨텍스트에 올리지 않는 값 조회여야 한다
     */
    Optional<Long> findPostIdOf(Long id);

    /**
     * 같은 글의 다른 교환(제외할 교환 ID가 아닌) 중 완료(COMPLETED) 상태가 있는지
     */
    boolean existsCompletedByPostIdExcludingExchange(Long postId, Long excludedExchangeId);
}
