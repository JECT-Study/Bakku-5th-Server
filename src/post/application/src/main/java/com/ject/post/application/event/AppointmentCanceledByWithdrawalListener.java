package com.ject.post.application.event;

import com.ject.common.event.exchange.AppointmentCanceledByMemberWithdrawal;
import com.ject.post.application.service.command.WithdrawnMemberReservedPostService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AppointmentCanceledByWithdrawalListener {

    private static final Logger log = LoggerFactory.getLogger(AppointmentCanceledByWithdrawalListener.class);

    private final WithdrawnMemberReservedPostService withdrawnMemberReservedPostService;

    @TransactionalEventListener
    public void handle(AppointmentCanceledByMemberWithdrawal event) {
        try {
            withdrawnMemberReservedPostService.settle(event.postIds(), event.withdrawnMemberId());
            log.info("탈퇴로 취소된 약속의 교환글 정리. appointmentId={}, postIds={}", event.appointmentId(),
                    event.postIds());
        } catch (RuntimeException e) {
            log.error("탈퇴로 취소된 약속의 교환글 정리 실패. appointmentId={}, postIds={}", event.appointmentId(),
                    event.postIds(), e);
        }
    }
}
