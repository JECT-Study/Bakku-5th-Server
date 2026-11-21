package com.ject.post.application.service.command;

import com.ject.common.core.cqrs.command.CommandHandler;
import com.ject.post.application.dto.command.CreatePostCommand;
import com.ject.post.application.dto.command.UpdatePostCommand;
import com.ject.post.application.port.PostImagePort;
import com.ject.post.domain.domain.Place;
import com.ject.post.domain.domain.Post;
import com.ject.post.domain.domain.PostCategory;
import com.ject.post.domain.domain.PostStatus;
import com.ject.post.domain.exception.PostNotFoundException;
import com.ject.post.domain.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdatePostHandler implements CommandHandler<UpdatePostCommand, Void> {

    private final PostRepository postRepository;
    private final PostImagePort postImagePort;

    @Override
    @Transactional
    public Void handle(UpdatePostCommand command) {
        Post post = postRepository.findById(command.postId())
                .filter(found -> found.getStatus() != PostStatus.DELETED)
                .orElseThrow(() -> new PostNotFoundException(command.postId()));

        CreatePostCommand.PlaceCommand place = command.place();
        post.update(
                command.editorId(),
                command.imageKeys(),
                command.title(),
                PostCategory.valueOf(command.category()),
                command.opened(),
                command.tags(),
                command.description(),
                command.wantedItem(),
                new Place(place.lat(), place.lng(), place.name(), place.regionName(), place.memo()));
        post.attachImages(postImagePort.attachToPost(post.getAuthorId(), post.getId(), post.getImageKeys()));

        postRepository.save(post);
        return null;
    }
}
