package com.ject.post.application.dto.command;

import com.ject.common.core.cqrs.command.Command;

public record IncreaseViewCountCommand(Long postId, Long viewerId) implements Command<Void> {
}
