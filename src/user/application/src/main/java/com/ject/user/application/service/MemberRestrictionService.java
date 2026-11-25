package com.ject.user.application.service;

import com.ject.common.core.time.ServiceTime;
import com.ject.user.application.dto.MemberRestriction;
import com.ject.user.application.port.MemberRepository;
import com.ject.user.application.query.MemberRestrictionQuery;
import com.ject.user.domain.domain.Member;
import com.ject.user.domain.exception.MemberNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 다른 BC에 주는 이용 제한 조회.
 */
@Service
@Transactional(readOnly = true)
public class MemberRestrictionService implements MemberRestrictionQuery {

    private final MemberRepository memberRepository;
    private final ServiceTime serviceTime;

    public MemberRestrictionService(MemberRepository memberRepository, ServiceTime serviceTime) {
        this.memberRepository = memberRepository;
        this.serviceTime = serviceTime;
    }

    @Override
    public MemberRestriction getRestriction(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException(memberId));
        if (!member.isRestrictedAt(serviceTime.now())) {
            return MemberRestriction.none();
        }
        return new MemberRestriction(true, null, member.getRestrictedUntil());
    }
}
