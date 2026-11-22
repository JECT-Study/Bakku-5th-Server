package com.ject.user.infrastructure.persistence.mapper;

import com.ject.user.application.port.BlockRepository;
import com.ject.user.domain.domain.Block;
import com.ject.user.infrastructure.persistence.entity.BlockJpaEntity;
import com.ject.user.infrastructure.persistence.repository.BlockJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class BlockRepositoryAdapter implements BlockRepository {

    private final BlockJpaRepository blockJpaRepository;

    public BlockRepositoryAdapter(BlockJpaRepository blockJpaRepository) {
        this.blockJpaRepository = blockJpaRepository;
    }

    @Override
    public boolean saveIfAbsent(Block block) {
        return blockJpaRepository.insertIfAbsent(block.getBlockerId(), block.getBlockedId(), block.getBlockedAt()) > 0;
    }

    @Override
    public Optional<Block> find(Long blockerId, Long blockedId) {
        return blockJpaRepository.findByBlockerIdAndBlockedId(blockerId, blockedId).map(BlockRepositoryAdapter::toDomain);
    }

    private static Block toDomain(BlockJpaEntity entity) {
        return Block.restore(entity.getId(), entity.getBlockerId(), entity.getBlockedId(), entity.getBlockedAt());
    }
}
