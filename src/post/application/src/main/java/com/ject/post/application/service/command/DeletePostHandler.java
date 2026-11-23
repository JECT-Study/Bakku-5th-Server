package com.ject.post.application.service.command;

import com.ject.common.core.cqrs.command.CommandHandler;
import com.ject.common.core.time.ServiceTime;
import com.ject.common.event.post.PostDeleted;
import com.ject.post.application.dto.command.DeletePostCommand;
import com.ject.post.domain.domain.Post;
import com.ject.post.domain.exception.PostNotFoundException;
import com.ject.post.domain.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeletePostHandler implements CommandHandler<DeletePostCommand, Void> {

    private final PostRepository postRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ServiceTime serviceTime;

    @Override
    @Transactional
    public Void handle(DeletePostCommand command) {
        Post post = postRepository.findById(command.postId())
                .orElseThrow(() -> new PostNotFoundException(command.postId()));
        if (!post.delete(command.requesterId(), serviceTime.now())) {
            return null;
        }
        postRepository.delete(post);
        eventPublisher.publishEvent(new PostDeleted(post.getId(), post.getAuthorId(), post.getDeletedAt()));
        return null;
    }
}
