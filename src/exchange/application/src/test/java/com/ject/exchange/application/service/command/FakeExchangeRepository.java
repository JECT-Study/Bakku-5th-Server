package com.ject.exchange.application.service.command;

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
 * 테스트용 가짜 교환 저장소. Handler가 무엇을 저장했는지 DB 없이 확인한다.
 * E-1처럼 id 없는 새 교환을 저장하면 DB처럼 교환 id와 아이템 id를 채워 돌려준다.
 */
class FakeExchangeRepository implements ExchangeRepository {

    private static final Instant SAVED_AT = Instant.parse("2026-11-21T05:00:00Z");
    private static final long FIRST_NEW_EXCHANGE_ID = 1_000L;
    private static final long FIRST_NEW_ITEM_ID = 5_000L;

    private final Map<Long, Exchange> store = new HashMap<>();
    private int saveCount = 0;
    private long nextExchangeId = FIRST_NEW_EXCHANGE_ID;
    private long nextItemId = FIRST_NEW_ITEM_ID;

    @Override
    public Exchange save(Exchange exchange) {
        Exchange saved = exchange.getId() == null ? assignIds(exchange) : exchange;
        store.put(saved.getId(), saved);
        saveCount++;
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

    /** 교환이 요청한 글 ID만 돌려준다. 교환이 없으면 빈 값이다. */
    @Override
    public Optional<Long> findPostIdOf(Long id) {
        return Optional.ofNullable(store.get(id)).map(Exchange::getPostId);
    }

    /** 테스트 준비용 저장. 저장 횟수에 포함하지 않는다. */
    void preload(Exchange exchange) {
        store.put(exchange.getId(), exchange);
    }

    int saveCount() {
        return saveCount;
    }

    /** 저장소에 남아 있는 교환 수. NEW 방식 E-1은 사진 확정 전후로 두 번 저장할 수 있어 저장 횟수 대신 쓴다. */
    int storedCount() {
        return store.size();
    }

    List<Exchange> storedExchanges() {
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
