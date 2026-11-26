package com.ject.post.application;

import com.ject.post.application.port.MemberRestrictionPort;

import java.util.HashSet;
import java.util.Set;

public class FakeMemberRestrictionPort implements MemberRestrictionPort {

    private final Set<Long> restricted = new HashSet<>();

    public FakeMemberRestrictionPort restrict(Long memberId) {
        restricted.add(memberId);
        return this;
    }

    @Override
    public boolean isRestricted(Long memberId) {
        return restricted.contains(memberId);
    }
}
