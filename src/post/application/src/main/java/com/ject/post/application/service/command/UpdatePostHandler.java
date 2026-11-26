package com.ject.post.application.service.command;

import com.ject.common.core.cqrs.command.CommandHandler;
import com.ject.common.core.time.ServiceTime;
import com.ject.common.event.post.PostUpdated;
import com.ject.post.application.dto.command.CreatePostCommand;
import com.ject.post.application.dto.command.UpdatePostCommand;
import com.ject.post.application.port.MemberRestrictionPort;
import com.ject.post.application.port.PostImagePort;
import com.ject.post.domain.domain.Place;
import com.ject.post.domain.domain.Post;
import com.ject.post.domain.domain.PostCategory;
import com.ject.post.domain.exception.RestrictedMemberException;
import com.ject.post.domain.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UpdatePostHandler implements CommandHandler<UpdatePostCommand, Void> {

    private final PostRepository postRepository;
    private final PostImagePort postImagePort;
    private final MemberRestrictionPort memberRestrictionPort;
    private final ApplicationEventPublisher eventPublisher;
    private final ServiceTime serviceTime;

    @Override
    @Transactional
    public Void handle(UpdatePostCommand command) {
        if (memberRestrictionPort.isRestricted(command.editorId())) {
            throw new RestrictedMemberException(command.editorId());
        }
        Post post = postRepository.getVisible(command.postId());
        List<String> imageKeysBefore = post.getImageKeys();

        CreatePostCommand.PlaceCommand place = command.place();
        boolean contentChanged = post.update(
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

        if (!contentChanged && imageKeysBefore.equals(post.getImageKeys())) {
            return null;
        }
        postRepository.save(post);
        eventPublisher.publishEvent(new PostUpdated(post.getId(), post.getAuthorId(), post.getTitle(), serviceTime.now()));
        return null;
    }
}
