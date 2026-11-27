package com.ject.exchange.application.service.contract;

import com.ject.common.core.time.ServiceTime;
import com.ject.exchange.application.contract.ChatRoomAppointmentState;
import com.ject.exchange.application.contract.ChatRoomExchangeQuery;
import com.ject.exchange.application.contract.ChatRoomExchangeState;
import com.ject.exchange.domain.domain.appointment.Appointment;
import com.ject.exchange.domain.domain.appointment.AppointmentStatus;
import com.ject.exchange.domain.domain.exchange.Exchange;
import com.ject.exchange.domain.exception.ExchangeErrorCode;
import com.ject.exchange.domain.exception.ExchangeException;
import com.ject.exchange.domain.repository.AppointmentRepository;
import com.ject.exchange.domain.repository.ExchangeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ChatRoomExchangeQueryService implements ChatRoomExchangeQuery {

    private final ExchangeRepository exchangeRepository;
    private final AppointmentRepository appointmentRepository;
    private final ServiceTime serviceTime;

    @Override
    @Transactional(readOnly = true)
    public ChatRoomExchangeState getChatRoomState(Long exchangeId, Long viewerId) {
        Exchange exchange = exchangeRepository.findById(exchangeId)
                .orElseThrow(() -> new ExchangeException(ExchangeErrorCode.EXCHANGE_NOT_FOUND));
        if (!exchange.isParticipant(viewerId)) {
            throw new ExchangeException(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT);
        }
        Instant now = serviceTime.now();
        boolean otherExchangeCompleted = exchangeRepository.existsCompletedByPostIdExcludingExchange(
                exchange.getPostId(), exchange.getId());
        ChatRoomAppointmentState appointmentState = Appointment
                .currentAmong(appointmentRepository.findAllByExchangeId(exchangeId), now)
                .filter(appointment -> appointment.getStatus() == AppointmentStatus.CONFIRMED)
                .map(appointment -> confirmationStateOf(appointment, now))
                .orElse(ChatRoomAppointmentState.NONE);
        return new ChatRoomExchangeState(otherExchangeCompleted, appointmentState);
    }

    private static ChatRoomAppointmentState confirmationStateOf(Appointment appointment, Instant now) {
        Instant opensAt = appointment.confirmationOpensAt();
        if (opensAt != null && !now.isBefore(opensAt)) {
            return ChatRoomAppointmentState.CONFIRMATION_OPENED;
        }
        return ChatRoomAppointmentState.CONFIRMED;
    }
}
