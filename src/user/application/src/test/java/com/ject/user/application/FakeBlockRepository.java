package com.ject.user.application;

import com.ject.user.application.port.BlockRepository;
import com.ject.user.domain.domain.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

class FakeBlockRepository implements BlockRepository {

    private final List<Block> store = new ArrayList<>();
    private long sequence = 0;

    @Override
    public boolean saveIfAbsent(Block block) {
        if (find(block.getBlockerId(), block.getBlockedId()).isPresent()) {
            return false;
        }
        store.add(Block.restore(++sequence, block.getBlockerId(), block.getBlockedId(), block.getBlockedAt()));
        return true;
    }

    @Override
    public Optional<Block> find(Long blockerId, Long blockedId) {
        return store.stream()
                .filter(block -> Objects.equals(block.getBlockerId(), blockerId)
                        && Objects.equals(block.getBlockedId(), blockedId))
                .findFirst();
    }

    int count() {
        return store.size();
    }
}
