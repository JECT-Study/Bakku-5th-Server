package com.ject.user.application.port;

import com.ject.user.domain.domain.Block;

import java.util.List;
import java.util.Optional;

/**
 * 차단 저장소 출력 포트. 구현은 infrastructure가 담당한다.
 */
public interface BlockRepository {

    /**
     * 같은 차단이 없을 때만 저장한다. 같은 요청이 동시에 와도 한 행만 남는다 (결정 C8).
     *
     * @return 새로 저장했으면 true, 이미 차단한 회원이면 false
     */
    boolean saveIfAbsent(Block block);

    Optional<Block> find(Long blockerId, Long blockedId);

    /**
     * @return 지웠으면 true, 차단하지 않은 회원이면 false
     */
    boolean delete(Long blockerId, Long blockedId);

    /**
     * 이 회원이 차단한 목록 중 탈퇴하지 않은 회원만, 최근 차단 순(id 내림차순)으로 (결정 A10).
     *
     * @param cursorId 이 id보다 작은 것부터. null이면 처음부터
     */
    List<Block> findPageOfActiveMembers(Long blockerId, Long cursorId, int limit);

    /**
     * 이 회원이 차단했거나 이 회원을 차단한 회원 id. 중복이 있을 수 있다.
     */
    List<Long> findRelatedMemberIds(Long memberId);

    /**
     * 두 회원 중 한쪽이라도 상대를 차단했는지.
     */
    boolean existsBetween(Long memberId, Long otherMemberId);
}
