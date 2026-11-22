package com.ject.exchange.application.service.command;

import com.ject.common.core.cqrs.command.CommandHandler;
import com.ject.common.core.time.ServiceTime;
import com.ject.exchange.application.dto.command.UpdateReminderCommand;
import com.ject.exchange.domain.domain.appointment.Appointment;
import com.ject.exchange.domain.domain.exchange.Exchange;
import com.ject.exchange.domain.exception.ExchangeErrorCode;
import com.ject.exchange.domain.exception.ExchangeException;
import com.ject.exchange.domain.repository.AppointmentRepository;
import com.ject.exchange.domain.repository.ExchangeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateReminderHandler implements CommandHandler<UpdateReminderCommand, Long> {

    private final AppointmentRepository appointmentRepository;
    private final ExchangeRepository exchangeRepository;
    private final ServiceTime serviceTime;

    @Override
    @Transactional
    public Long handle(UpdateReminderCommand command) {
        // 한 약속의 내 알림 칸만 바꾸므로 약속 행 잠금만 잡는다 (application 규약 "동시 변경 보호")
        Appointment appointment = appointmentRepository.findByIdForUpdate(command.appointmentId())
                .orElseThrow(() -> new ExchangeException(ExchangeErrorCode.APPOINTMENT_NOT_FOUND));
        Exchange exchange = exchangeRepository.findById(appointment.getExchangeId())
                .orElseThrow(() -> new ExchangeException(ExchangeErrorCode.EXCHANGE_NOT_FOUND));
        if (!exchange.isParticipant(command.memberId())) {
            throw new ExchangeException(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT);
        }
        exchange.validateActive();

        Appointment changed = appointment.changeReminder(
                command.memberId(), command.reminderOffsetMinutes(), serviceTime.now());
        // Todo: 약속 전 알림 재예약 (E-06-1, 예약 기능 연동 전)
        return appointmentRepository.save(changed).getId();
    }
}
