package com.ject.user.application.port;

import com.ject.user.domain.domain.Block;

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
}
