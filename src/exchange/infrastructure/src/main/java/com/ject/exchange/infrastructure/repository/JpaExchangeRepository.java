package com.ject.exchange.infrastructure.repository;

import com.ject.exchange.domain.domain.exchange.Exchange;
import com.ject.exchange.domain.repository.ExchangeRepository;
import com.ject.exchange.infrastructure.entity.ExchangeEntity;
import com.ject.exchange.infrastructure.entity.OfferedItemEntity;
import com.ject.exchange.infrastructure.jpa.ExchangeEntityRepository;
import com.ject.exchange.infrastructure.jpa.OfferedItemEntityRepository;
import com.ject.exchange.infrastructure.mapper.ExchangeMapper;
import com.ject.exchange.infrastructure.mapper.OfferedItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaExchangeRepository implements ExchangeRepository {

    private final ExchangeEntityRepository exchangeEntityRepository;
    private final OfferedItemEntityRepository offeredItemEntityRepository;

    @Override
    @Transactional
    public Exchange save(Exchange exchange) {
        ExchangeEntity save = exchangeEntityRepository.save(ExchangeMapper.toEntity(exchange));
        List<OfferedItemEntity> items = offeredItemEntityRepository.saveAll(
                OfferedItemMapper.toEntities(save, exchange));
        return ExchangeMapper.toDomain(save, items);
    }

    @Override
    @Transactional
    public Optional<Exchange> findById(Long id) {
        return exchangeEntityRepository.findById(id)
                .map(e -> ExchangeMapper.toDomain(
                        e, offeredItemEntityRepository.findByExchangeIdOrderBySortOrder(id)
                ));
    }
}
