package com.ject.user.domain.domain;

import com.ject.user.domain.exception.SelfBlockException;

import java.time.Instant;
import java.util.Objects;

/**
 * 회원 차단. 한 사람이 같은 회원을 한 번만 차단하고, 차단 관계는 양방향으로 본다 (C-09-2).
 */
public class Block {

    private final Long id;
    private final Long blockerId;
    private final Long blockedId;
    private final Instant blockedAt;

    private Block(Long id, Long blockerId, Long blockedId, Instant blockedAt) {
        this.id = id;
        this.blockerId = Objects.requireNonNull(blockerId);
        this.blockedId = Objects.requireNonNull(blockedId);
        this.blockedAt = Objects.requireNonNull(blockedAt);
    }

    /**
     * @throws SelfBlockException 자기 자신을 차단할 때 (MEMBER_401)
     */
    public static Block create(Long blockerId, Long blockedId, Instant now) {
        if (Objects.equals(blockerId, blockedId)) {
            throw new SelfBlockException(blockerId);
        }
        return new Block(null, blockerId, blockedId, now);
    }

    // 저장소에서 읽어온 데이터로 복원할 때 사용
    public static Block restore(Long id, Long blockerId, Long blockedId, Instant blockedAt) {
        return new Block(id, blockerId, blockedId, blockedAt);
    }

    public Long getId() {
        return id;
    }

    public Long getBlockerId() {
        return blockerId;
    }

    public Long getBlockedId() {
        return blockedId;
    }

    public Instant getBlockedAt() {
        return blockedAt;
    }
}
