package com.ject.exchange.application.service.command;

import com.ject.common.core.cqrs.command.CommandHandler;
import com.ject.common.core.time.ServiceTime;
import com.ject.exchange.application.dto.command.WithdrawAppointmentCommand;
import com.ject.exchange.domain.domain.appointment.Appointment;
import com.ject.exchange.domain.domain.exchange.Exchange;
import com.ject.exchange.domain.exception.ExchangeErrorCode;
import com.ject.exchange.domain.exception.ExchangeException;
import com.ject.exchange.domain.repository.AppointmentRepository;
import com.ject.exchange.domain.repository.ExchangeRepository;
import com.ject.exchange.domain.repository.PostLockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WithdrawAppointmentHandler implements CommandHandler<WithdrawAppointmentCommand, Long> {

    private final AppointmentRepository appointmentRepository;
    private final ExchangeRepository exchangeRepository;
    private final PostLockRepository postLockRepository;
    private final ServiceTime serviceTime;

    @Override
    @Transactional
    public Long handle(WithdrawAppointmentCommand command) {
        // 수락의 확정 · 자동 거절, 거절과 겹쳐도 한쪽 결과가 덮이지 않도록 같은 글 잠금을 먼저 잡고, 그 뒤에 약속을 처음 읽는다
        List<Long> postIds = appointmentRepository.findPostIdsOf(command.appointmentId());
        postLockRepository.lockAll(postIds);
        Appointment appointment = appointmentRepository.findByIdForUpdate(command.appointmentId())
                .orElseThrow(() -> new ExchangeException(ExchangeErrorCode.APPOINTMENT_NOT_FOUND));
        Exchange exchange = exchangeRepository.findById(appointment.getExchangeId())
                .orElseThrow(() -> new ExchangeException(ExchangeErrorCode.EXCHANGE_NOT_FOUND));
        if (!exchange.isParticipant(command.withdrawerId())) {
            throw new ExchangeException(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT);
        }
        appointment.validateProposer(command.withdrawerId());
        exchange.validateActive();

        Appointment withdrawn = appointment.withdraw(command.withdrawerId(), serviceTime.now());
        // Todo: 채팅방 카드 갱신 (받는 사람 알림은 보내지 않는다, P-07-04)
        return appointmentRepository.save(withdrawn).getId();
    }
}
