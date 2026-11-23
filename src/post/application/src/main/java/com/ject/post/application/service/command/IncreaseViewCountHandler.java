package com.ject.post.application.service.command;

import com.ject.common.core.cqrs.command.CommandHandler;
import com.ject.common.core.time.ServiceTime;
import com.ject.post.application.dto.command.IncreaseViewCountCommand;
import com.ject.post.application.port.PostViewLog;
import com.ject.post.domain.domain.Post;
import com.ject.post.domain.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IncreaseViewCountHandler implements CommandHandler<IncreaseViewCountCommand, Void> {

    private final PostRepository postRepository;
    private final PostViewLog postViewLog;
    private final ServiceTime serviceTime;

    @Override
    @Transactional
    public Void handle(IncreaseViewCountCommand command) {
        Post post = postRepository.getVisible(command.postId());
        if (!post.isWrittenBy(command.viewerId())
                && postViewLog.markViewed(command.postId(), command.viewerId(), serviceTime.today())) {
            postRepository.increaseViewCount(command.postId());
        }
        return null;
    }
}
