package com.ject.user.domain.exception;

import com.ject.common.core.exception.DomainException;

/**
 * 탈퇴 · 영구 정지 회원의 프로필을 볼 때 (MEMBER_202). 영구 정지 회원도 탈퇴와 같게 보인다 (명세 MemberStatus.BANNED).
 */
public class WithdrawnMemberException extends DomainException {

    public WithdrawnMemberException(Long memberId) {
        super(UserErrorCode.MEMBER_WITHDRAWN, "탈퇴한 회원입니다. id=" + memberId);
    }
}
