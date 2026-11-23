package com.ject.exchange.api.controller;

import com.ject.exchange.domain.domain.exchange.Exchange;
import com.ject.exchange.domain.domain.exchange.OfferedItem;
import com.ject.exchange.domain.repository.ExchangeRepository;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * 컨트롤러 테스트용 가짜 교환 저장소. Fake 버스가 실제 Handler를 거치게 해 에러 코드와 응답 흐름을 함께 확인한다.
 * E-1처럼 id 없는 새 교환을 저장하면 DB처럼 교환 id와 아이템 id를 채워 돌려준다.
 */
class FakeExchangeRepository implements ExchangeRepository {

    private static final Instant SAVED_AT = Instant.parse("2026-11-21T05:00:00Z");
    private static final long FIRST_NEW_EXCHANGE_ID = 1_000L;
    private static final long FIRST_NEW_ITEM_ID = 5_000L;

    private final Map<Long, Exchange> store = new HashMap<>();
    private long nextExchangeId = FIRST_NEW_EXCHANGE_ID;
    private long nextItemId = FIRST_NEW_ITEM_ID;

    @Override
    public Exchange save(Exchange exchange) {
        Exchange saved = exchange.getId() == null ? assignIds(exchange) : exchange;
        store.put(saved.getId(), saved);
        return saved;
    }

    @Override
    public Optional<Exchange> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    /** 잠금은 DB에서만 의미가 있으므로 Fake는 findById와 같다. */
    @Override
    public Optional<Exchange> findByIdForUpdate(Long id) {
        return findById(id);
    }

    boolean isEmpty() {
        return store.isEmpty();
    }

    List<Exchange> findAll() {
        return List.copyOf(store.values());
    }

    private Exchange assignIds(Exchange exchange) {
        List<OfferedItem> items = exchange.getOfferedItems();
        List<OfferedItem> itemsWithId = IntStream.range(0, items.size())
                .mapToObj(index -> {
                    OfferedItem item = items.get(index);
                    return OfferedItem.of(nextItemId++, item.getSourcePostId(), item.getName(), item.getOpened(),
                            item.getDescription(), item.getSortOrder(), item.getImageKeys());
                })
                .toList();
        return Exchange.of(nextExchangeId++, exchange.getPostId(), exchange.getRequestUserId(),
                exchange.getOwnerId(), exchange.getOfferType(), itemsWithId, exchange.getStatus(),
                null, SAVED_AT, SAVED_AT);
    }
}
