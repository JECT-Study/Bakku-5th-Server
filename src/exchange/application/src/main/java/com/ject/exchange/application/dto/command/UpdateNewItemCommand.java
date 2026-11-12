package com.ject.exchange.application.dto.command;

import com.ject.common.core.cqrs.command.Command;
import com.ject.exchange.application.dto.dto.NewItem;

public record UpdateNewItemCommand(
        Long exchangeId,

        Long requesterId,

        NewItem newItem
) implements Command<Long> {
}
