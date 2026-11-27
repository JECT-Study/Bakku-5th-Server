package com.ject.chat.application.port;

import com.ject.chat.application.dto.ChatMemberInfo;
import com.ject.chat.application.dto.ChatMemberRestriction;

/**
 * user BC의 회원 정보를 조회하는 Port
 */
public interface ChatMemberPort {

    /**
     * 회원 요약을 조회한다. 탈퇴한 회원도 {@link ChatMemberInfo#withdrawn()} true로 반환한다
     */
    ChatMemberInfo getSummary(Long memberId);

    /**
     * 두 회원 중 한쪽이라도 상대를 차단했는지 (양방향)
     */
    boolean isBlocked(Long memberId, Long counterpartId);

    /**
     * 회원의 이용 제한 여부를 조회한다
     */
    ChatMemberRestriction getRestriction(Long memberId);
}
