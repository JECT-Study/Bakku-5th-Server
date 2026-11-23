package com.ject.exchange.application.service.command;

import com.ject.common.core.cqrs.command.CommandHandler;
import com.ject.exchange.application.dto.command.UpdateNewItemCommand;
import com.ject.exchange.application.port.ExchangeImageRejectedException;
import com.ject.exchange.application.port.ImagePort;
import com.ject.exchange.domain.domain.exchange.Exchange;
import com.ject.exchange.domain.domain.exchange.vo.ItemDescription;
import com.ject.exchange.domain.domain.exchange.vo.ItemName;
import com.ject.exchange.domain.exception.ExchangeErrorCode;
import com.ject.exchange.domain.exception.ExchangeException;
import com.ject.exchange.domain.repository.ExchangeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UpdateNewItemHandler implements CommandHandler<UpdateNewItemCommand, Long> {

    private final ExchangeRepository exchangeRepository;
    private final ImagePort imagePort;

    @Override
    @Transactional
    public Long handle(UpdateNewItemCommand command) {
        Exchange exchange = exchangeRepository.findById(command.exchangeId())
                .orElseThrow(() -> new ExchangeException(ExchangeErrorCode.EXCHANGE_NOT_FOUND));

        Exchange updatedExchange = exchange.updateNewItem(
                command.requesterId(),
                new ItemName(command.newItem().name()),
                command.newItem().opened(),
                new ItemDescription(command.newItem().description())
        );
        // 기존 판정(012 · 013 · 010)을 통과한 뒤에만 사진을 확정한다 (spec AC-7)
        List<String> imageKeys = attachImages(command.requesterId(), exchange.getId(), command.newItem().imageKeys());
        // Todo: 목록에서 빠진 사진은 저장소에서 지우지 않는다 (post 글 수정과 같음, spec 결정 3). 지우려면 커밋 뒤 discardImages
        return exchangeRepository.save(updatedExchange.withNewItemImages(imageKeys)).getId();
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
