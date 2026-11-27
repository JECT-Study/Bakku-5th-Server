package com.ject.user.application;

import com.ject.user.application.dto.MemberProfile;
import com.ject.user.application.service.MemberProfileService;
import com.ject.user.domain.domain.BirthDate;
import com.ject.user.domain.domain.Block;
import com.ject.user.domain.domain.MannerScore;
import com.ject.user.domain.domain.Member;
import com.ject.user.domain.domain.MemberStatus;
import com.ject.user.domain.domain.Nickname;
import com.ject.user.domain.domain.SocialProvider;
import com.ject.user.domain.exception.BlockedMemberException;
import com.ject.user.domain.exception.MemberNotFoundException;
import com.ject.user.domain.exception.WithdrawnMemberException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemberProfileServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-27T03:00:00Z");

    private FakeMemberRepository memberRepository;
    private FakeBlockRepository blockRepository;
    private MemberProfileService memberProfileService;
    private Long viewerId;

    @BeforeEach
    void setUp() {
        memberRepository = new FakeMemberRepository();
        blockRepository = new FakeBlockRepository(memberRepository);
        memberProfileService = new MemberProfileService(memberRepository, blockRepository);
        viewerId = save("보는사람", MemberStatus.ACTIVE, 1, 50);
    }

    @Test
    @DisplayName("다른 회원 프로필은 닉네임 · 완료 횟수와 매너 지표 구간 이름만 준다")
    void otherMember() {
        // given
        Long ownerId = save("게시자", MemberStatus.ACTIVE, 5, 82);

        // when
        MemberProfile profile = memberProfileService.getProfile(viewerId, ownerId);

        // then
        assertEquals("게시자", profile.nickname());
        assertEquals(5, profile.completedCount());
        assertFalse(profile.isMe());
        assertEquals("PRO_COLLECTOR", profile.mannerTier());
    }

    @Test
    @DisplayName("교환 0회면 매너 지표 대신 안내 문구를 보여 주므로 구간 이름이 null이다")
    void noExchange() {
        // given
        Long ownerId = save("첫교환", MemberStatus.ACTIVE, 0, 50);

        // when
        MemberProfile profile = memberProfileService.getProfile(viewerId, ownerId);

        // then
        assertNull(profile.mannerTier());
    }

    @Test
    @DisplayName("내 프로필이면 isMe=true")
    void me() {
        // when
        MemberProfile profile = memberProfileService.getProfile(viewerId, viewerId);

        // then
        assertTrue(profile.isMe());
    }

    @Test
    @DisplayName("없는 회원이면 MEMBER_201")
    void notFound() {
        assertThrows(MemberNotFoundException.class, () -> memberProfileService.getProfile(viewerId, 999L));
    }

    @Test
    @DisplayName("탈퇴한 회원이면 MEMBER_202")
    void withdrawn() {
        // given
        Long ownerId = save("떠난회원", MemberStatus.ACTIVE, 3, 50);
        Member owner = memberRepository.findById(ownerId).orElseThrow();
        owner.withdraw(NOW);
        memberRepository.save(owner);

        // when · then
        assertThrows(WithdrawnMemberException.class, () -> memberProfileService.getProfile(viewerId, ownerId));
    }

    @Test
    @DisplayName("영구 정지 회원도 탈퇴와 같게 MEMBER_202")
    void banned() {
        // given
        Long ownerId = save("정지회원", MemberStatus.BANNED, 3, 50);

        // when · then
        assertThrows(WithdrawnMemberException.class, () -> memberProfileService.getProfile(viewerId, ownerId));
    }

    @Test
    @DisplayName("내가 차단한 회원이면 MEMBER_203")
    void blockedByMe() {
        // given
        Long ownerId = save("차단당함", MemberStatus.ACTIVE, 3, 50);
        blockRepository.saveIfAbsent(Block.create(viewerId, ownerId, NOW));

        // when · then
        assertThrows(BlockedMemberException.class, () -> memberProfileService.getProfile(viewerId, ownerId));
    }

    @Test
    @DisplayName("나를 차단한 회원이어도 MEMBER_203")
    void blockedMe() {
        // given
        Long ownerId = save("차단함", MemberStatus.ACTIVE, 3, 50);
        blockRepository.saveIfAbsent(Block.create(ownerId, viewerId, NOW));

        // when · then
        assertThrows(BlockedMemberException.class, () -> memberProfileService.getProfile(viewerId, ownerId));
    }

    private Long save(String nickname, MemberStatus status, int completedCount, int mannerScore) {
        Member member = Member.restore(null, SocialProvider.KAKAO, "hash-" + nickname, new Nickname(nickname),
                new BirthDate(LocalDate.of(2000, 1, 1)), null, status, null, completedCount,
                new MannerScore(mannerScore), false, NOW, null, null, NOW, null);
        return memberRepository.save(member).getId();
    }
}
