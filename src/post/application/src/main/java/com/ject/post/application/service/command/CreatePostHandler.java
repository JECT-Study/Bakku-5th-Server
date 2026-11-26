package com.ject.post.application.service.command;

import com.ject.common.core.cqrs.command.CommandHandler;
import com.ject.common.core.time.ServiceTime;
import com.ject.post.application.dto.command.CreatePostCommand;
import com.ject.post.application.port.MemberRestrictionPort;
import com.ject.post.application.port.PostImagePort;
import com.ject.post.domain.domain.Place;
import com.ject.post.domain.domain.Post;
import com.ject.post.domain.domain.PostCategory;
import com.ject.post.domain.exception.RestrictedMemberException;
import com.ject.post.domain.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreatePostHandler implements CommandHandler<CreatePostCommand, Long> {

    private final PostRepository postRepository;
    private final PostImagePort postImagePort;
    private final MemberRestrictionPort memberRestrictionPort;
    private final ServiceTime serviceTime;

    @Override
    @Transactional
    public Long handle(CreatePostCommand command) {
        if (memberRestrictionPort.isRestricted(command.authorId())) {
            throw new RestrictedMemberException(command.authorId());
        }
        CreatePostCommand.PlaceCommand place = command.place();
        Post post = Post.create(
                command.authorId(),
                command.imageKeys(),
                command.title(),
                PostCategory.valueOf(command.category()),
                command.opened(),
                command.tags(),
                command.description(),
                command.wantedItem(),
                new Place(place.lat(), place.lng(), place.name(), place.regionName(), place.memo()),
                command.nowExchangeMinutes(),
                serviceTime.now());

        Post saved = postRepository.save(post);
        saved.attachImages(postImagePort.attachToPost(saved.getAuthorId(), saved.getId(), saved.getImageKeys()));
        return postRepository.save(saved).getId();
    }
}
