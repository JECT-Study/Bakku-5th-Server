package com.ject.post.application.service.query;

import com.ject.common.core.cqrs.query.QueryHandler;
import com.ject.common.core.time.ServiceTime;
import com.ject.post.application.dto.AuthorInfo;
import com.ject.post.application.dto.PostResult;
import com.ject.post.application.dto.query.GetPostQuery;
import com.ject.post.application.port.AuthorPort;
import com.ject.post.domain.domain.Place;
import com.ject.post.domain.domain.Post;
import com.ject.post.domain.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetPostHandler implements QueryHandler<GetPostQuery, PostResult> {

    private final PostRepository postRepository;
    private final AuthorPort authorPort;
    private final ServiceTime serviceTime;

    @Override
    @Transactional(readOnly = true)
    public PostResult handle(GetPostQuery query) {
        Post post = postRepository.getVisible(query.postId());
        AuthorInfo author = authorPort.findAuthors(List.of(post.getAuthorId())).get(post.getAuthorId());
        return toResult(post, author, query.viewerId());
    }

    private PostResult toResult(Post post, AuthorInfo author, Long viewerId) {
        Place place = post.getPlace();
        return new PostResult(
                post.getId(),
                post.getStatus().name(),
                post.getImageKeys(),
                post.getTitle(),
                post.getCategory().name(),
                post.isOpened(),
                post.getTags(),
                post.getDescription(),
                post.getWantedItem(),
                new PostResult.PlaceResult(place.lat(), place.lng(), place.name(),
                        place.regionName(), place.memo()),
                post.isNowExchange(serviceTime.now()),
                serviceTime.toOffset(post.getNowExchangeEndsAt()),
                0,
                post.getViewCount(),
                author,
                post.isWrittenBy(viewerId),
                null,
                post.canBeRequestedBy(viewerId),
                serviceTime.toOffset(post.getCreatedAt()));
    }
}
