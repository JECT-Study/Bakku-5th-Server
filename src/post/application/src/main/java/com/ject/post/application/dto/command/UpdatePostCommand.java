package com.ject.post.application.dto.command;

import com.ject.common.core.cqrs.command.Command;

import java.util.List;

public record UpdatePostCommand(
        Long postId,
        Long editorId,
        List<String> imageKeys,
        String title,
        String category,
        boolean opened,
        List<String> tags,
        String description,
        String wantedItem,
        CreatePostCommand.PlaceCommand place
) implements Command<Void> {
}
