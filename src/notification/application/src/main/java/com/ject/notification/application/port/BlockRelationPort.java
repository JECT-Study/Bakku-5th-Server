package com.ject.notification.application.port;

import java.util.List;

/**
 * 차단 관계 조회 (회원 BC). 내가 차단했거나 나를 차단한 회원 모두 해당한다.
 */
public interface BlockRelationPort {

    List<Long> getBlockRelatedMemberIds(Long memberId);
}
