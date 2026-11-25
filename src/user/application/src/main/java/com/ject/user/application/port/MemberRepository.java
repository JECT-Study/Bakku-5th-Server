package com.ject.user.application.port;

import com.ject.user.domain.domain.Member;
import com.ject.user.domain.domain.Nickname;
import com.ject.user.domain.domain.SocialProvider;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 회원 저장소 출력 포트. 구현은 infrastructure가 담당한다.
 */
public interface MemberRepository {

    Member save(Member member);

    Optional<Member> findById(Long id);

    /**
     * 영문 대소문자를 구분하지 않고 찾는다 (결정 A5-2). 탈퇴 회원은 닉네임이 비어 있어 찾지 않는다.
     */
    Optional<Member> findByNickname(Nickname nickname);

    /**
     * 탈퇴하지 않은 회원 중 이 소셜 계정의 회원. 한 소셜 계정은 탈퇴하지 않은 회원 하나에만 연결된다 (결정 C9).
     */
    Optional<Member> findActiveBySocialIdHash(SocialProvider provider, String socialIdHash);

    /**
     * 이 소셜 계정으로 가장 최근에 탈퇴한 회원. 재가입 7일 확인에 쓴다 (A-03-1).
     */
    Optional<Member> findLatestWithdrawnBySocialIdHash(SocialProvider provider, String socialIdHash);

    /**
     * 있는 회원만 반환하며 순서는 보장하지 않는다.
     */
    List<Member> findAllByIds(Collection<Long> ids);

    /**
     * 교환 완료 횟수를 1씩 올린다. 동시에 끝난 교환이 서로 덮어쓰지 않도록 DB에서 직접 더한다 (결정 C8).
     */
    void increaseCompletedCount(Collection<Long> ids);

    /**
     * 마지막 닉네임 변경이 changedBefore 이전(또는 없음)일 때만 바꾼다. 동시에 바꿔도 한 번만 반영된다 (결정 C8).
     *
     * @return 바꿨으면 true, 그사이 다른 요청이 먼저 바꿔 조건이 안 맞으면 false
     * @throws com.ject.user.domain.exception.DuplicateNicknameException 그사이 다른 회원이 같은 닉네임을 쓴 경우 (MEMBER_102)
     */
    boolean changeNickname(Long id, Nickname nickname, Instant changedAt, Instant changedBefore);
}
