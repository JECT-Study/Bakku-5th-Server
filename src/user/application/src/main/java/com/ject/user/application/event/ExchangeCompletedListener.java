package com.ject.user.application.event;

import com.ject.common.event.exchange.ExchangeCompleted;
import com.ject.user.application.port.MemberRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 교환 완료를 받아 양쪽 회원의 교환 완료 횟수를 올린다 (결정 B7).
 * 교환 트랜잭션이 커밋된 뒤 처리하므로 새 트랜잭션을 연다.
 */
@Component
public class ExchangeCompletedListener {

    private final MemberRepository memberRepository;

    public ExchangeCompletedListener(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(ExchangeCompleted event) {
        memberRepository.increaseCompletedCount(event.memberIds());
    }
}
