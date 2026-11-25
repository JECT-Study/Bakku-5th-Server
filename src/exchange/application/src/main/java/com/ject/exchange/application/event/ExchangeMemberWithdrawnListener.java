package com.ject.exchange.application.event;

import com.ject.common.event.member.MemberWithdrawn;
import com.ject.exchange.application.service.command.WithdrawnMemberAppointmentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ExchangeMemberWithdrawnListener {

    private static final Logger log = LoggerFactory.getLogger(ExchangeMemberWithdrawnListener.class);

    private final WithdrawnMemberAppointmentService withdrawnMemberAppointmentService;

    @TransactionalEventListener
    public void handle(MemberWithdrawn event) {
        try {
            withdrawnMemberAppointmentService.cancelActiveAppointments(event.memberId(), event.withdrawnAt());
        } catch (RuntimeException e) {
            log.error("탈퇴 회원 약속 취소 실패. memberId={}", event.memberId(), e);
        }
    }
}
