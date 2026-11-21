package com.ject.post.application.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record PostResult(
        Long id,
        String status,
        List<String> imageKeys,
        String title,
        String category,
        boolean opened,
        List<String> tags,
        String description,
        String wantedItem,
        PlaceResult place,
        boolean nowExchange,
        OffsetDateTime nowExchangeEndsAt,
        int requestCount,
        int viewCount,
        AuthorInfo author,
        boolean isMine,
        Long chatRoomId,
        boolean canRequest,
        OffsetDateTime createdAt
) {

    public record PlaceResult(double lat, double lng, String name, String regionName, String memo) {
    }
}