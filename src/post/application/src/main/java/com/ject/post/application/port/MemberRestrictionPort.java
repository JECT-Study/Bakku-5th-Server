package com.ject.post.application.port;

public interface MemberRestrictionPort {
    boolean isRestricted(Long memberId);
}
