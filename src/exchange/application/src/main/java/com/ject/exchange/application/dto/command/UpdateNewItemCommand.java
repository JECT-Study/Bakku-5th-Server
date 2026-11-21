package com.ject.exchange.application.dto.command;

import com.ject.common.core.cqrs.command.Command;
import com.ject.exchange.application.dto.dto.NewItem;

public record UpdateNewItemCommand(
        /**
         * 수정할 교환 ID
         */
        Long exchangeId,

        /**
         * 수정을 요청한 회원 ID. 교환 요청자 본인이어야 한다
         */
        Long requesterId,

        /**
         * 바꿀 아이템 값. 제품명 · 개봉 여부 · 설명
         */
        NewItem newItem
) implements Command<Long> {
}
