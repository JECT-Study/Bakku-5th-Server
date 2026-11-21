package com.ject.user.application.event;

import com.ject.common.event.exchange.ExchangeCompleted;
import com.ject.user.application.service.CompletedCountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 교환 완료를 받아 양쪽 회원의 교환 완료 횟수를 올린다 (결정 B7).
 */
@Component
public class ExchangeCompletedListener {

    private static final Logger log = LoggerFactory.getLogger(ExchangeCompletedListener.class);

    private final CompletedCountService completedCountService;

    public ExchangeCompletedListener(CompletedCountService completedCountService) {
        this.completedCountService = completedCountService;
    }

    /**
     * 교환은 이미 커밋돼서 되돌릴 수 없으므로, 실패하면 ERROR 로그로 남겨 직접 맞출 수 있게 하고 예외는 넘기지 않는다.
     * 넘기면 교환 완료 요청이 성공했는데도 오류 응답이 나간다.
     */
    @TransactionalEventListener
    public void handle(ExchangeCompleted event) {
        try {
            completedCountService.increase(event.memberIds());
        } catch (RuntimeException e) {
            log.error("교환 완료 횟수 증가 실패. appointmentId={}, memberIds={}", event.appointmentId(),
                    event.memberIds(), e);
        }
    }
}
