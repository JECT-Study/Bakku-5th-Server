package com.ject.user.application.service;

import com.ject.user.application.dto.MemberProfile;
import com.ject.user.application.port.BlockRepository;
import com.ject.user.application.port.MemberRepository;
import com.ject.user.domain.domain.Member;
import com.ject.user.domain.exception.BlockedMemberException;
import com.ject.user.domain.exception.MemberNotFoundException;
import com.ject.user.domain.exception.WithdrawnMemberException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 다른 회원 프로필 조회 (MB-4). 게시자 프로필(C-05-2)에 쓴다.
 */
@Service
@Transactional(readOnly = true)
public class MemberProfileService {

    private final MemberRepository memberRepository;
    private final BlockRepository blockRepository;

    public MemberProfileService(MemberRepository memberRepository, BlockRepository blockRepository) {
        this.memberRepository = memberRepository;
        this.blockRepository = blockRepository;
    }

    /**
     * 차단하면 서로의 글 · 채팅이 숨겨져 정상 경로로는 들어올 수 없지만, 주소로 직접 부르는 경우를 막는다 (C-09-2).
     *
     * @param viewerId 로그인한 회원
     * @param userId   볼 회원
     * @throws MemberNotFoundException  없는 회원 (MEMBER_201)
     * @throws WithdrawnMemberException 탈퇴 · 영구 정지 회원 (MEMBER_202)
     * @throws BlockedMemberException   내가 차단했거나 나를 차단한 회원 (MEMBER_203)
     */
    public MemberProfile getProfile(Long viewerId, Long userId) {
        Member member = memberRepository.findById(userId).orElseThrow(() -> new MemberNotFoundException(userId));
        if (member.appearsWithdrawn()) {
            throw new WithdrawnMemberException(userId);
        }
        boolean isMe = userId.equals(viewerId);
        if (!isMe && blockRepository.existsBetween(viewerId, userId)) {
            throw new BlockedMemberException(userId);
        }
        return MemberProfile.of(member, isMe);
    }
}
