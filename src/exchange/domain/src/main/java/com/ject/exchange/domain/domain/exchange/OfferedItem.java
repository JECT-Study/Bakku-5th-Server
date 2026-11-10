package com.ject.exchange.domain.domain.exchange;

import com.ject.exchange.domain.domain.exchange.vo.ItemDescription;
import com.ject.exchange.domain.domain.exchange.vo.ItemName;
import com.ject.exchange.domain.exception.ExchangeErrorCode;
import com.ject.exchange.domain.exception.ExchangeException;
import lombok.Getter;

@Getter
public class OfferedItem {

    private final Long id;
    private final Long sourcePostId;
    private final ItemName name;
    private final ItemCondition itemCondition;
    private final ItemDescription description;
    private final int sortOrder;

    private OfferedItem(Long id, Long sourcePostId, ItemName name, ItemCondition itemCondition, ItemDescription description, int sortOrder) {
        this.id = id;
        this.sourcePostId = sourcePostId;
        this.name = name;
        this.itemCondition = itemCondition;
        this.description = description;
        this.sortOrder = sortOrder;
    }

    public static OfferedItem of(Long id, Long sourcePostId, ItemName name, ItemCondition itemCondition, ItemDescription description, int sortOrder) {
        return new OfferedItem(id, sourcePostId, name, itemCondition, description, sortOrder);
    }

    public static OfferedItem ofPost(Long sourcePostId, int sortOrder) {
        // region validation
        if (sourcePostId == null) {
            throw new ExchangeException(ExchangeErrorCode.INVALID_OFFERED_ITEMS);
        }
        // endregion
        return new OfferedItem(null, sourcePostId, null, null, null, sortOrder);
    }

    public static OfferedItem ofNew(ItemName name, ItemCondition itemCondition, ItemDescription description, int sortOrder) {
        if (name == null || itemCondition == null || description == null) {
            throw new ExchangeException(ExchangeErrorCode.INVALID_OFFERED_ITEMS);
        }
        return new OfferedItem(null, null, name, itemCondition, description, sortOrder);
    }
}
