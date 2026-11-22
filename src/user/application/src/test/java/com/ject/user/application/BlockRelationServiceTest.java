package com.ject.user.application;

import com.ject.user.application.service.BlockRelationService;
import com.ject.user.domain.domain.Block;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockRelationServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-22T03:00:00Z");

    private FakeBlockRepository blockRepository;
    private BlockRelationService blockRelationService;

    @BeforeEach
    void setUp() {
        blockRepository = new FakeBlockRepository(new FakeMemberRepository());
        blockRelationService = new BlockRelationService(blockRepository);
    }

    @Test
    @DisplayName("내가 차단한 회원과 나를 차단한 회원을 모두 주고, 서로 차단했어도 한 번만 준다")
    void getBlockRelatedMemberIds() {
        // given
        block(1L, 2L);
        block(3L, 1L);
        block(1L, 4L);
        block(4L, 1L);
        block(2L, 3L);

        // when
        List<Long> ids = blockRelationService.getBlockRelatedMemberIds(1L);

        // then
        assertEquals(List.of(2L, 3L, 4L), ids.stream().sorted().toList());
    }

    @Test
    @DisplayName("차단 관계가 없으면 빈 목록")
    void getBlockRelatedMemberIds_none() {
        assertTrue(blockRelationService.getBlockRelatedMemberIds(1L).isEmpty());
    }

    @Test
    @DisplayName("한쪽만 차단해도 양쪽 모두 차단 관계로 본다")
    void isBlocked_eitherDirection() {
        // given
        block(1L, 2L);

        // when & then
        assertTrue(blockRelationService.isBlocked(1L, 2L));
        assertTrue(blockRelationService.isBlocked(2L, 1L));
        assertFalse(blockRelationService.isBlocked(1L, 3L));
    }

    private void block(Long blockerId, Long blockedId) {
        blockRepository.saveIfAbsent(Block.create(blockerId, blockedId, NOW));
    }
}
