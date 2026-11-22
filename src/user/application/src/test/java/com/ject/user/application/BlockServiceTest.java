package com.ject.user.application;

import com.ject.common.core.time.ServiceTime;
import com.ject.common.event.member.MemberBlocked;
import com.ject.user.application.dto.BlockResult;
import com.ject.user.application.service.BlockService;
import com.ject.user.domain.domain.BirthDate;
import com.ject.user.domain.domain.Block;
import com.ject.user.domain.domain.Member;
import com.ject.user.domain.domain.MemberStatus;
import com.ject.user.domain.domain.Nickname;
import com.ject.user.domain.domain.SocialProvider;
import com.ject.user.domain.exception.MemberNotFoundException;
import com.ject.user.domain.exception.SelfBlockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-22T03:00:00Z");
    private static final Instant JOINED_AT = Instant.parse("2026-11-01T03:00:00Z");

    private FakeMemberRepository memberRepository;
    private FakeBlockRepository blockRepository;
    private List<Object> publishedEvents;
    private BlockService blockService;
    private Long me;
    private Long other;

    @BeforeEach
    void setUp() {
        memberRepository = new FakeMemberRepository();
        blockRepository = new FakeBlockRepository();
        publishedEvents = new ArrayList<>();
        blockService = new BlockService(blockRepository, memberRepository, publishedEvents::add,
                new ServiceTime(Clock.fixed(NOW, ZoneId.of("Asia/Seoul"))));
        me = memberRepository.save(member("쿠로미덕후")).getId();
        other = memberRepository.save(member("하치와레러버")).getId();
    }

    @Test
    @DisplayName("BL-1 차단하면 저장하고 MemberBlocked를 발행한다")
    void block() {
        // when
        BlockResult result = blockService.block(me, other);

        // then
        assertEquals(new BlockResult(other, NOW, true), result);
        assertTrue(blockRepository.find(me, other).isPresent());
        assertEquals(List.of(new MemberBlocked(me, other, NOW)), publishedEvents);
    }

    @Test
    @DisplayName("BL-1 이미 차단한 회원이면 기존 차단 시각을 주고 이벤트는 다시 발행하지 않는다")
    void block_alreadyBlocked() {
        // given
        Instant blockedBefore = NOW.minusSeconds(3600);
        blockRepository.saveIfAbsent(Block.restore(null, me, other, blockedBefore));

        // when
        BlockResult result = blockService.block(me, other);

        // then
        assertEquals(new BlockResult(other, blockedBefore, false), result);
        assertEquals(1, blockRepository.count());
        assertTrue(publishedEvents.isEmpty());
    }

    @Test
    @DisplayName("BL-1 자기 자신을 차단하면 SelfBlockException")
    void block_self() {
        assertThrows(SelfBlockException.class, () -> blockService.block(me, me));
        assertTrue(publishedEvents.isEmpty());
    }

    @Test
    @DisplayName("BL-1 없는 회원이면 MemberNotFoundException")
    void block_unknownMember() {
        assertThrows(MemberNotFoundException.class, () -> blockService.block(me, 999L));
        assertEquals(0, blockRepository.count());
    }

    @Test
    @DisplayName("BL-1 탈퇴한 회원이면 MemberNotFoundException")
    void block_withdrawnMember() {
        // given
        Member withdrawn = memberRepository.findById(other).orElseThrow();
        withdrawn.withdraw(NOW);
        memberRepository.save(withdrawn);

        // when & then
        assertThrows(MemberNotFoundException.class, () -> blockService.block(me, other));
        assertFalse(blockRepository.find(me, other).isPresent());
    }

    private static Member member(String nickname) {
        return Member.restore(null, SocialProvider.KAKAO, "hash-" + nickname, new Nickname(nickname),
                new BirthDate(LocalDate.of(2001, 3, 15)), null, MemberStatus.ACTIVE, null, 0, false, JOINED_AT, null,
                null, JOINED_AT, null);
    }
}
