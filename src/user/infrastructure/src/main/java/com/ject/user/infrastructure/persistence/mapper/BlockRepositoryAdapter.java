package com.ject.user.infrastructure.persistence.mapper;

import com.ject.user.application.port.BlockRepository;
import com.ject.user.domain.domain.Block;
import com.ject.user.domain.domain.MemberStatus;
import com.ject.user.infrastructure.persistence.entity.BlockJpaEntity;
import com.ject.user.infrastructure.persistence.repository.BlockJpaRepository;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;

import java.util.List;
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
        return blockJpaRepository.findByBlockerIdAndBlockedId(blockerId, blockedId)
                .map(BlockRepositoryAdapter::toDomain);
    }

    @Override
    public boolean delete(Long blockerId, Long blockedId) {
        return blockJpaRepository.delete(blockerId, blockedId) > 0;
    }

    @Override
    public List<Block> findPageOfActiveMembers(Long blockerId, Long cursorId, int limit) {
        Long before = cursorId == null ? Long.MAX_VALUE : cursorId;
        return blockJpaRepository.findPage(blockerId, before, MemberStatus.WITHDRAWN, Limit.of(limit)).stream()
                .map(BlockRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public List<Long> findRelatedMemberIds(Long memberId) {
        return blockJpaRepository.findRelatedMemberIds(memberId);
    }

    @Override
    public boolean existsBetween(Long memberId, Long otherMemberId) {
        return blockJpaRepository.existsBetween(memberId, otherMemberId);
    }

    private static Block toDomain(BlockJpaEntity entity) {
        return Block.restore(entity.getId(), entity.getBlockerId(), entity.getBlockedId(), entity.getBlockedAt());
    }
}
