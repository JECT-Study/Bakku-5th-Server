package com.ject.exchange.application.dto.command;

import com.ject.common.core.cqrs.command.Command;
import com.ject.exchange.domain.domain.exchange.ItemCondition;
import com.ject.exchange.domain.domain.exchange.OfferType;
import com.ject.exchange.domain.exception.ExchangeErrorCode;
import com.ject.exchange.domain.exception.ExchangeException;

import java.util.List;

public record CreateExchangeCommand(
        /**
         * 교환글 요청자 ID
         */
        Long requesterId,
        /**
         * 요청할 교환글 ID
         */
        Long postId,
        /**
         * 첨부 방식. POST 내 교환글 / NEW 새로 작성
         */
        OfferType offerType,
        /**
         * POST일 때 필수. 내 교환글 ID 1~5개, 중복 불가. NEW면 null
         */
        List<Long> offeredPostIds,
        /**
         * NEW일 때 필수. POST면 null
         */
        NewItem newItem
) implements Command<Long> {

    public record NewItem(String name, ItemCondition condition, String description) {}

    public CreateExchangeCommand {
        if(!isValidOfferType(offerType, offeredPostIds, newItem)) {
            throw new ExchangeException(ExchangeErrorCode.INVALID_OFFERED_ITEMS);
        }
    }

    private static boolean isValidOfferType(OfferType offerType, List<Long> offeredPostIds, NewItem newItem) {
        return switch (offerType) {
            case POST -> offeredPostIds != null && !offeredPostIds.isEmpty() && newItem == null;
            case NEW -> newItem != null && (offeredPostIds == null || offeredPostIds.isEmpty());
        };
    }
}
