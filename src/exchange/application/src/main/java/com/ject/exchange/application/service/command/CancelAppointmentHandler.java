package com.ject.exchange.application.service.command;

import com.ject.common.core.cqrs.command.CommandHandler;
import com.ject.common.core.time.ServiceTime;
import com.ject.exchange.application.dto.command.CancelAppointmentCommand;
import com.ject.exchange.application.port.PostPort;
import com.ject.exchange.domain.domain.appointment.Appointment;
import com.ject.exchange.domain.domain.appointment.AppointmentStatus;
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
public class CancelAppointmentHandler implements CommandHandler<CancelAppointmentCommand, Long> {

    private final AppointmentRepository appointmentRepository;
    private final ExchangeRepository exchangeRepository;
    private final PostLockRepository postLockRepository;
    private final PostPort postPort;
    private final ServiceTime serviceTime;

    @Override
    @Transactional
    public Long handle(CancelAppointmentCommand command) {
        // 취소하면 교환글 · 고른 첨부 글이 교환 가능으로 돌아가므로 다른 방의 수락과 겹치지 않게 글을 먼저 잠그고,
        // 두 사람의 동시 취소는 약속 행 잠금으로 한 줄로 세운다 (F-1과 같은 순서)
        List<Long> postIds = appointmentRepository.findPostIdsOf(command.appointmentId());
        postLockRepository.lockAll(postIds);
        Appointment appointment = appointmentRepository.findByIdForUpdate(command.appointmentId())
                .orElseThrow(() -> new ExchangeException(ExchangeErrorCode.APPOINTMENT_NOT_FOUND));
        Exchange exchange = exchangeRepository.findById(appointment.getExchangeId())
                .orElseThrow(() -> new ExchangeException(ExchangeErrorCode.EXCHANGE_NOT_FOUND));
        if (!exchange.isParticipant(command.cancelerId())) {
            throw new ExchangeException(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT);
        }
        // 이미 끝난 약속은 교환이 종료돼 있어도 "이미 처리된 약속"으로 안내한다 (E-07-1 예외)
        if (appointment.getStatus() == AppointmentStatus.CONFIRMED) {
            exchange.validateActive();
        }

        Appointment canceled = appointment.cancel(command.cancelerId(), serviceTime.now());
        // post 요청이 실패하면 취소도 남지 않아야 하므로 저장보다 먼저 요청한다 (F-1과 같음)
        postPort.release(postIds);
        // Todo: 채팅방 "약속이 취소됐어요" 카드 · 상대 알림 · 같은 글 다른 방 "다시 약속을 잡을 수 있어요", 예약 알림 · 교환 확인 취소
        return appointmentRepository.save(canceled).getId();
    }
}
