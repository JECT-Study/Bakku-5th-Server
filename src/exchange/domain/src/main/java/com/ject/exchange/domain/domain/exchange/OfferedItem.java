package com.ject.exchange.domain.domain.exchange;

import com.ject.exchange.domain.domain.exchange.vo.ItemDescription;
import com.ject.exchange.domain.domain.exchange.vo.ItemName;
import com.ject.exchange.domain.exception.ExchangeErrorCode;
import com.ject.exchange.domain.exception.ExchangeException;
import lombok.Getter;

import java.util.List;

@Getter
public class OfferedItem {

    private final Long id;
    private final Long sourcePostId;
    private final ItemName name;
    private final Boolean opened;
    private final ItemDescription description;
    private final int sortOrder;
    /** 새로 작성한 아이템의 정식 사진 key. 순서대로, 첫 장이 대표. POST 아이템 · 사진 없음은 빈 목록 */
    private final List<String> imageKeys;

    private OfferedItem(Long id, Long sourcePostId, ItemName name, Boolean opened, ItemDescription description, int sortOrder,
                        List<String> imageKeys) {
        this.id = id;
        this.sourcePostId = sourcePostId;
        this.name = name;
        this.opened = opened;
        this.description = description;
        this.sortOrder = sortOrder;
        this.imageKeys = List.copyOf(imageKeys);
    }

    public static OfferedItem of(Long id, Long sourcePostId, ItemName name, Boolean opened, ItemDescription description, int sortOrder) {
        return new OfferedItem(id, sourcePostId, name, opened, description, sortOrder, List.of());
    }

    public static OfferedItem of(Long id, Long sourcePostId, ItemName name, Boolean opened, ItemDescription description, int sortOrder,
                                 List<String> imageKeys) {
        return new OfferedItem(id, sourcePostId, name, opened, description, sortOrder, imageKeys);
    }

    public static OfferedItem ofPost(Long sourcePostId, int sortOrder) {
        // region validation
        if (sourcePostId == null) {
            throw new ExchangeException(ExchangeErrorCode.INVALID_OFFERED_ITEMS);
        }
        // endregion
        return new OfferedItem(null, sourcePostId, null, null, null, sortOrder, List.of());
    }

    public static OfferedItem ofNew(ItemName name, Boolean opened, ItemDescription description, int sortOrder) {
        if (name == null || opened == null || description == null) {
            throw new ExchangeException(ExchangeErrorCode.INVALID_OFFERED_ITEMS);
        }
        return new OfferedItem(null, null, name, opened, description, sortOrder, List.of());
    }

    public OfferedItem updateNew(ItemName name, Boolean opened, ItemDescription description) {
        if (name == null || opened == null || description == null) {
            throw new ExchangeException(ExchangeErrorCode.INVALID_OFFERED_ITEMS);
        }
        return new OfferedItem(id, null, name, opened, description, sortOrder, imageKeys);
    }

    public OfferedItem withImageKeys(List<String> imageKeys) {
        return new OfferedItem(id, sourcePostId, name, opened, description, sortOrder, imageKeys);
    }
}
