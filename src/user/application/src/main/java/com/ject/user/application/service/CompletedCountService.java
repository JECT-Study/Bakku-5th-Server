package com.ject.user.application.service;

import com.ject.user.application.port.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

/**
 * 교환 완료 횟수 +1 (결정 B7). ExchangeCompletedListener가 부른다.
 * 리스너가 실패를 잡아 로그로 남기려면 트랜잭션이 리스너 밖(이 클래스)에서 끝나야 해서 따로 둔다.
 */
@Service
public class CompletedCountService {

    private final MemberRepository memberRepository;

    public CompletedCountService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    /**
     * 교환 트랜잭션이 커밋된 뒤 불리므로 새 트랜잭션을 연다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void increase(Collection<Long> memberIds) {
        memberRepository.increaseCompletedCount(memberIds);
    }
}
