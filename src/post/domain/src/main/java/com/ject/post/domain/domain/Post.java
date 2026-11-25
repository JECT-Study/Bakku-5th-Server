package com.ject.post.domain.domain;

import com.ject.post.domain.exception.NotPostAuthorException;
import com.ject.post.domain.exception.PostCompletedException;
import com.ject.post.domain.exception.PostNotFoundException;
import com.ject.post.domain.exception.PostReservedException;
import com.ject.post.domain.exception.PostStatusTransitionException;
import lombok.Builder;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
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
    private Instant deletedAt;

    @Builder
    private Post(Long id, Long authorId, PostStatus status, List<String> imageKeys, List<String> tags, String title,
                 PostCategory category, boolean opened, String description, String wantedItem, Place place,
                 Instant nowExchangeEndsAt, int viewCount, Instant createdAt, Instant deletedAt) {
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
        this.deletedAt = deletedAt;
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

    // 사진은 최종 key로 비교해야하기 때문에 반환값에 미포함
    public boolean update(Long editorId, List<String> imageKeys, String title, PostCategory category, boolean opened,
                          List<String> tags, String description, String wantedItem, Place place) {
        if (!isWrittenBy(editorId)) {
            throw NotPostAuthorException.onUpdate(id);
        }
        if (status == PostStatus.RESERVED) {
            throw PostReservedException.onUpdate(id);
        }
        if (status == PostStatus.COMPLETED) {
            throw PostCompletedException.onUpdate(id);
        }
        List<String> newTags = distinctTags(tags);
        boolean changed = !Objects.equals(this.title, title)
                || this.category != category
                || this.opened != opened
                || !this.tags.equals(newTags)
                || !Objects.equals(this.description, description)
                || !Objects.equals(this.wantedItem, wantedItem)
                || !Objects.equals(this.place, place);
        this.imageKeys = List.copyOf(imageKeys);
        this.title = title;
        this.category = category;
        this.opened = opened;
        this.tags = newTags;
        this.description = description;
        this.wantedItem = wantedItem;
        this.place = place;
        return changed;
    }

    public boolean delete(Long requesterId, Instant now) {
        if (status == PostStatus.DELETED) {
            if (!isWrittenBy(requesterId)) {
                throw new PostNotFoundException(id);
            }
            return false;
        }
        if (!isWrittenBy(requesterId)) {
            throw NotPostAuthorException.onDelete(id);
        }
        if (status == PostStatus.RESERVED) {
            throw PostReservedException.onDelete(id);
        }
        if (status == PostStatus.COMPLETED) {
            throw PostCompletedException.onDelete(id);
        }
        this.status = PostStatus.DELETED;
        this.deletedAt = now;
        return true;
    }

    public void reserve() {
        changeStatus(PostStatus.AVAILABLE, PostStatus.RESERVED);
    }

    public void release() {
        changeStatus(PostStatus.RESERVED, PostStatus.AVAILABLE);
    }

    public void complete() {
        changeStatus(PostStatus.RESERVED, PostStatus.COMPLETED);
    }

    private void changeStatus(PostStatus from, PostStatus to) {
        if (status == PostStatus.DELETED) {
            throw new PostNotFoundException(id);
        }
        if (status != from) {
            throw new PostStatusTransitionException(id);
        }
        this.status = to;
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