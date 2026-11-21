package com.ject.post.domain.domain;

import com.ject.post.domain.exception.NotPostAuthorException;
import com.ject.post.domain.exception.PostCompletedException;
import com.ject.post.domain.exception.PostReservedException;
import lombok.Builder;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Getter
public class Post {

    private final Long id;
    private final Long authorId;
    private PostStatus status;
    private List<String> imageKeys;
    private List<String> tags;
    private String title;
    private PostCategory category;
    private boolean opened;
    private String description;
    private String wantedItem;
    private Place place;
    private Instant nowExchangeEndsAt;
    private int viewCount;
    private final Instant createdAt;

    @Builder
    private Post(Long id, Long authorId, PostStatus status, List<String> imageKeys, List<String> tags, String title,
                 PostCategory category, boolean opened, String description, String wantedItem, Place place,
                 Instant nowExchangeEndsAt, int viewCount, Instant createdAt) {
        this.id = id;
        this.authorId = authorId;
        this.status = status;
        this.imageKeys = List.copyOf(imageKeys);
        this.tags = tags == null ? List.of() : List.copyOf(tags);
        this.title = title;
        this.category = category;
        this.opened = opened;
        this.description = description;
        this.wantedItem = wantedItem;
        this.place = place;
        this.nowExchangeEndsAt = nowExchangeEndsAt;
        this.viewCount = viewCount;
        this.createdAt = createdAt;
    }

    public static Post create(Long authorId, List<String> imageKeys, String title, PostCategory category,
                              boolean opened, List<String> tags, String description, String wantedItem,
                              Place place, Integer nowExchangeMinutes, Instant now) {
        Instant nowExchangeEndsAt = nowExchangeMinutes == null
                ? null
                : now.plus(Duration.ofMinutes(nowExchangeMinutes));

        return Post.builder()
                .authorId(authorId)
                .status(PostStatus.AVAILABLE)
                .imageKeys(imageKeys)
                .tags(distinctTags(tags))
                .title(title)
                .category(category)
                .opened(opened)
                .description(description)
                .wantedItem(wantedItem)
                .place(place)
                .nowExchangeEndsAt(nowExchangeEndsAt)
                .viewCount(0)
                .build();
    }

    public void update(Long editorId, List<String> imageKeys, String title, PostCategory category, boolean opened,
                       List<String> tags, String description, String wantedItem, Place place) {
        if (!isWrittenBy(editorId)) {
            throw new NotPostAuthorException(id);
        }
        if (status == PostStatus.RESERVED) {
            throw new PostReservedException(id);
        }
        if (status == PostStatus.COMPLETED) {
            throw new PostCompletedException(id);
        }
        this.imageKeys = List.copyOf(imageKeys);
        this.title = title;
        this.category = category;
        this.opened = opened;
        this.tags = distinctTags(tags);
        this.description = description;
        this.wantedItem = wantedItem;
        this.place = place;
    }

    public void attachImages(List<String> finalKeys) {
        if (id == null) {
            throw new IllegalStateException("저장 전에는 사진을 연결할 수 없습니다.");
        }
        this.imageKeys = List.copyOf(finalKeys);
    }

    public boolean isWrittenBy(Long memberId) {
        return authorId.equals(memberId);
    }

    public boolean canBeRequestedBy(Long memberId) {
        return !isWrittenBy(memberId) && status == PostStatus.AVAILABLE;
    }

    public boolean isNowExchange(Instant now) {
        return isNowExchange(status, nowExchangeEndsAt, now);
    }

    public static boolean isNowExchange(PostStatus status, Instant nowExchangeEndsAt, Instant now) {
        return status == PostStatus.AVAILABLE
                && nowExchangeEndsAt != null
                && now.isBefore(nowExchangeEndsAt);
    }

    private static List<String> distinctTags(List<String> tags) {
        if (tags == null) {
            return List.of();
        }
        Set<String> distinct = new LinkedHashSet<>();
        for (String tag : tags) {
            distinct.add(TagName.normalize(tag));
        }
        return List.copyOf(distinct);
    }
}