package com.ject.exchange.application.service.command;

import com.ject.common.core.cqrs.command.CommandHandler;
import com.ject.exchange.application.dto.command.UpdateNewItemCommand;
import com.ject.exchange.domain.domain.exchange.Exchange;
import com.ject.exchange.domain.domain.exchange.vo.ItemDescription;
import com.ject.exchange.domain.domain.exchange.vo.ItemName;
import com.ject.exchange.domain.exception.ExchangeErrorCode;
import com.ject.exchange.domain.exception.ExchangeException;
import com.ject.exchange.domain.repository.ExchangeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateNewItemHandler implements CommandHandler<UpdateNewItemCommand, Long> {

    private final ExchangeRepository exchangeRepository;

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
        exchangeRepository.save(updatedExchange);
        return updatedExchange.getId();
    }
}
