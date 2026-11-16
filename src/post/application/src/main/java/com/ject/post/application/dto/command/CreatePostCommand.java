package com.ject.post.application.dto.command;

import com.ject.common.core.cqrs.command.Command;

import java.util.List;

public record CreatePostCommand(
        Long authorId,
        List<String> imageKeys,
        String title,
        String category,
        boolean opened,
        List<String> tags,
        String description,
        String wantedItem,
        PlaceCommand place,
        Integer nowExchangeMinutes
) implements Command<Long> {
    public record PlaceCommand(double lat, double lng, String name, String regionName, String memo) {}
}
