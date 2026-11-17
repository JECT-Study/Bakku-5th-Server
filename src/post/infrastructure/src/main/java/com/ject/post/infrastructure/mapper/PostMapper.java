package com.ject.post.infrastructure.mapper;

import com.ject.post.domain.domain.Place;
import com.ject.post.domain.domain.Post;
import com.ject.post.infrastructure.entity.PlaceEmbeddable;
import com.ject.post.infrastructure.entity.PostEntity;

import java.util.List;

public final class PostMapper {

    private PostMapper() {
    }

    public static Post toDomain(PostEntity entity, List<String> imageKeys, List<String> tags) {
        PlaceEmbeddable place = entity.getPlace();
        return Post.builder()
                .id(entity.getId())
                .authorId(entity.getAuthorId())
                .status(entity.getStatus())
                .imageKeys(imageKeys)
                .tags(tags)
                .title(entity.getTitle())
                .category(entity.getCategory())
                .opened(entity.isOpened())
                .description(entity.getDescription())
                .wantedItem(entity.getWantedItem())
                .place(new Place(place.getLat(), place.getLng(), place.getName(),
                        place.getRegionName(), place.getMemo()))
                .nowExchangeEndsAt(entity.getNowExchangeEndsAt())
                .viewCount(entity.getViewCount())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
