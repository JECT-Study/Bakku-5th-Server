package com.ject.post.application.dto.command;

import com.ject.common.core.cqrs.command.Command;

public record DeletePostCommand(Long postId, Long requesterId) implements Command<Void> {
}
