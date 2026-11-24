package com.ject.exchange.application.service.command;

import com.ject.common.core.cqrs.command.CommandHandler;
import com.ject.exchange.application.dto.command.UpdateNewItemCommand;
import com.ject.exchange.application.port.ExchangeImageRejectedException;
import com.ject.exchange.application.port.ImagePort;
import com.ject.exchange.domain.domain.appointment.AppointmentStatus;
import com.ject.exchange.domain.domain.exchange.Exchange;
import com.ject.exchange.domain.domain.exchange.vo.ItemDescription;
import com.ject.exchange.domain.domain.exchange.vo.ItemName;
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
public class UpdateNewItemHandler implements CommandHandler<UpdateNewItemCommand, Long> {

    private final ExchangeRepository exchangeRepository;
    private final AppointmentRepository appointmentRepository;
    private final PostLockRepository postLockRepository;
    private final ImagePort imagePort;

    @Override
    @Transactional
    public Long handle(UpdateNewItemCommand command) {
        // 수락(A-4) · 교환 확인(F-1)과 겹쳐도 확정 뒤 수정이나 완료 상태 덮어쓰기가 없도록 같은 글 잠금을 먼저 잡고,
        // 교환은 잠금 뒤에 처음 읽는다 (application 규약 "동시 변경 보호", spec 결정 4)
        Long postId = exchangeRepository.findPostIdOf(command.exchangeId())
                .orElseThrow(() -> new ExchangeException(ExchangeErrorCode.EXCHANGE_NOT_FOUND));
        postLockRepository.lockAll(List.of(postId));
        Exchange exchange = exchangeRepository.findById(command.exchangeId())
                .orElseThrow(() -> new ExchangeException(ExchangeErrorCode.EXCHANGE_NOT_FOUND));

        Exchange updatedExchange = exchange.updateNewItem(
                command.requesterId(),
                new ItemName(command.newItem().name()),
                command.newItem().opened(),
                new ItemDescription(command.newItem().description())
        );
        // 확정 약속이 있으면 새로 작성한 제품도 잠긴다 (C-07-2). 교환은 약속을 모르므로 여기서 본다 (#120 spec 결정 3)
        validateNoConfirmedAppointment(exchange.getId());
        // 기존 판정(012 · 013 · 010 · 014)을 통과한 뒤에만 사진을 확정한다 (#108 spec AC-7)
        List<String> imageKeys = attachImages(command.requesterId(), exchange.getId(), command.newItem().imageKeys());
        // Todo: 목록에서 빠진 사진은 저장소에서 지우지 않는다 (post 글 수정과 같음, spec 결정 3). 지우려면 커밋 뒤 discardImages
        return exchangeRepository.save(updatedExchange.withNewItemImages(imageKeys)).getId();
    }

    private void validateNoConfirmedAppointment(Long exchangeId) {
        boolean confirmed = appointmentRepository.findAllByExchangeId(exchangeId).stream()
                .anyMatch(appointment -> appointment.getStatus() == AppointmentStatus.CONFIRMED);
        if (confirmed) {
            throw new ExchangeException(ExchangeErrorCode.NEW_ITEM_LOCKED);
        }
    }

    private List<String> attachImages(Long requesterId, Long exchangeId, List<String> imageKeys) {
        try {
            return imagePort.attachToExchange(requesterId, exchangeId, imageKeys);
        } catch (ExchangeImageRejectedException rejected) {
            throw new ExchangeException(ExchangeErrorCode.INVALID_IMAGE_KEY,
                    rejected.getOrder() + "번째 이미지를 사용할 수 없습니다.");
        }
    }
}
