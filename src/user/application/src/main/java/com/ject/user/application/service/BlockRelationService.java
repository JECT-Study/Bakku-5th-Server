package com.ject.user.application.service;

import com.ject.user.application.port.BlockRepository;
import com.ject.user.application.query.BlockRelationQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 다른 BC에 주는 차단 관계 조회. 내가 차단했거나 나를 차단한 회원 모두 해당한다 (C-09-2).
 */
@Service
@Transactional(readOnly = true)
public class BlockRelationService implements BlockRelationQuery {

    private final BlockRepository blockRepository;

    public BlockRelationService(BlockRepository blockRepository) {
        this.blockRepository = blockRepository;
    }

    @Override
    public List<Long> getBlockRelatedMemberIds(Long memberId) {
        return blockRepository.findRelatedMemberIds(memberId).stream().distinct().toList();
    }

    @Override
    public boolean isBlocked(Long memberId, Long otherMemberId) {
        if (Objects.equals(memberId, otherMemberId)) {
            return false;
        }
        return blockRepository.existsBetween(memberId, otherMemberId);
    }
}
