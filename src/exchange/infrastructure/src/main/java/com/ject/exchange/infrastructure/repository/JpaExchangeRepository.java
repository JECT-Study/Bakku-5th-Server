package com.ject.exchange.infrastructure.repository;

import com.ject.exchange.domain.domain.exchange.Exchange;
import com.ject.exchange.domain.domain.exchange.ExchangeStatus;
import com.ject.exchange.domain.repository.ExchangeRepository;
import com.ject.exchange.infrastructure.entity.ExchangeEntity;
import com.ject.exchange.infrastructure.entity.OfferedItemEntity;
import com.ject.exchange.infrastructure.entity.OfferedItemImageEntity;
import com.ject.exchange.infrastructure.jpa.ExchangeEntityRepository;
import com.ject.exchange.infrastructure.jpa.OfferedItemEntityRepository;
import com.ject.exchange.infrastructure.jpa.OfferedItemImageEntityRepository;
import com.ject.exchange.infrastructure.mapper.ExchangeMapper;
import com.ject.exchange.infrastructure.mapper.OfferedItemMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class JpaExchangeRepository implements ExchangeRepository {

    private final ExchangeEntityRepository exchangeEntityRepository;
    private final OfferedItemEntityRepository offeredItemEntityRepository;
    private final OfferedItemImageEntityRepository offeredItemImageEntityRepository;

    @Override
    @Transactional
    public Exchange save(Exchange exchange) {
        ExchangeEntity save = exchangeEntityRepository.save(ExchangeMapper.toEntity(exchange));
        List<OfferedItemEntity> items = offeredItemEntityRepository.saveAll(
                OfferedItemMapper.toEntities(save, exchange));
        saveImages(items, exchange);
        return findById(save.getId()).orElseThrow();
    }

    /**
     * 아이템 사진은 목록째 바꾼다. 도메인 아이템과 저장된 아이템은 같은 순서다
     */
    private void saveImages(List<OfferedItemEntity> items, Exchange exchange) {
        List<Long> itemIds = items.stream().map(OfferedItemEntity::getId).toList();
        offeredItemImageEntityRepository.deleteByOfferedItemIds(itemIds);
        List<OfferedItemImageEntity> images = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            images.addAll(OfferedItemMapper.toImageEntities(items.get(index).getId(),
                    exchange.getOfferedItems().get(index).getImageKeys()));
        }
        offeredItemImageEntityRepository.saveAll(images);
    }

    private Exchange toDomain(ExchangeEntity entity) {
        List<OfferedItemEntity> items = offeredItemEntityRepository.findByExchangeIdOrderBySortOrder(entity.getId());
        Map<Long, List<String>> imageKeysByItemId = offeredItemImageEntityRepository
                .findByOfferedItemIdInOrderBySortOrder(items.stream().map(OfferedItemEntity::getId).toList()).stream()
                .collect(Collectors.groupingBy(OfferedItemImageEntity::getOfferedItemId,
                        Collectors.mapping(OfferedItemImageEntity::getImageKey, Collectors.toList())));
        return ExchangeMapper.toDomain(entity, items, imageKeysByItemId);
    }

    @Override
    @Transactional
    public Optional<Exchange> findById(Long id) {
        return exchangeEntityRepository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional
    public Optional<Exchange> findByIdForUpdate(Long id) {
        return exchangeEntityRepository.findWithLockById(id).map(this::toDomain);
    }

    @Override
    public Optional<Long> findPostIdOf(Long id) {
        return exchangeEntityRepository.findPostIdById(id);
    }

    @Override
    public boolean existsCompletedByPostIdExcludingExchange(Long postId, Long excludedExchangeId) {
        return exchangeEntityRepository.existsByPostIdAndStatusAndIdNot(postId, ExchangeStatus.COMPLETED, excludedExchangeId);
    }
}
