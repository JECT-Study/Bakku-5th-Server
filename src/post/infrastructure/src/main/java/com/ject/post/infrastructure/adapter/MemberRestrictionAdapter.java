package com.ject.post.infrastructure.adapter;

import com.ject.post.application.port.MemberRestrictionPort;
import com.ject.user.application.query.MemberRestrictionQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberRestrictionAdapter implements MemberRestrictionPort {

    private final MemberRestrictionQuery memberRestrictionQuery;

    @Override
    public boolean isRestricted(Long memberId) {
        return memberRestrictionQuery.getRestriction(memberId).restricted();
    }
}
