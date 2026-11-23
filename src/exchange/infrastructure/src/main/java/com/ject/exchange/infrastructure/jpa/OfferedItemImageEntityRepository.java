package com.ject.exchange.infrastructure.jpa;

import com.ject.exchange.infrastructure.entity.OfferedItemImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface OfferedItemImageEntityRepository extends JpaRepository<OfferedItemImageEntity, Long> {

    List<OfferedItemImageEntity> findByOfferedItemIdInOrderBySortOrder(Collection<Long> offeredItemIds);

    @Modifying(flushAutomatically = true)
    @Query("delete from OfferedItemImageEntity image where image.offeredItemId in :offeredItemIds")
    void deleteByOfferedItemIds(@Param("offeredItemIds") Collection<Long> offeredItemIds);
}
