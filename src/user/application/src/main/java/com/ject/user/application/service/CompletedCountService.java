package com.ject.user.application.service;

import com.ject.user.application.port.MemberRepository;
import com.ject.user.domain.domain.MannerScore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

/**
 * 교환 완료 횟수 +1 · 매너 지표 반영 (결정 B7, F-06-1). ExchangeCompletedListener가 부른다.
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
     * 매너 지표 +2는 [교환했어요] 답으로 완료된 교환만 받는다. 자동 완료 · 질문 뒤 나가기 · 교환 완료로 바꾸기는
     * 혼자서도 완료할 수 있어 완료 횟수만 올린다 (F-06-1, G-02-3).
     *
     * @param answered [교환했어요] 답으로 완료됐으면 true
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void increase(Collection<Long> memberIds, boolean answered) {
        memberRepository.increaseCompletedCount(memberIds, answered ? MannerScore.EXCHANGE_COMPLETED_POINTS : 0);
    }
}
