package com.ject.exchange.application.dto.dto;

public record PostInfo(
        Long id,
        String title,
        String thumbnailUrl,
        Long ownerId,
        Status status
) {

    public enum Status {
        AVAILABLE,
        RESERVED,
        COMPLETED,
        DELETED
    }

    public boolean isRequestable() {
        return status == Status.AVAILABLE;
    }

    public boolean isAttachableBy(Long requesterId) {
        return ownerId.equals(requesterId) && status == Status.AVAILABLE;
    }
}
