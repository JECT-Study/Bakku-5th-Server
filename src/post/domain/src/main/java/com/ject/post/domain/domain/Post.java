package com.ject.post.domain.domain;

import com.ject.post.domain.exception.InvalidImageKeyException;
import lombok.Builder;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Getter
public class Post {

    private static final String TMP_KEY_PREFIX = "post/tmp/%d/";
    private static final String POST_KEY_PREFIX = "post/%d/";

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
        validateUploadedBy(authorId, imageKeys);
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

    public void moveImagesToPost() {
        if (id == null) {
            throw new IllegalStateException("저장 전에는 사진을 옮길 수 없습니다.");
        }
        String tmpPrefix = TMP_KEY_PREFIX.formatted(authorId);
        String postPrefix = POST_KEY_PREFIX.formatted(id);
        List<String> moved = new ArrayList<>();
        for (String key : imageKeys) {
            moved.add(key.startsWith(tmpPrefix) ? postPrefix + key.substring(tmpPrefix.length()) : key);
        }
        this.imageKeys = List.copyOf(moved);
    }

    public boolean isNowExchange(Instant now) {
        return status == PostStatus.AVAILABLE
                && nowExchangeEndsAt != null
                && now.isBefore(nowExchangeEndsAt);
    }

    private static void validateUploadedBy(Long authorId, List<String> imageKeys) {
        String tmpPrefix = TMP_KEY_PREFIX.formatted(authorId);
        for (int i = 0; i < imageKeys.size(); i++) {
            String key = imageKeys.get(i);
            String fileName = key.startsWith(tmpPrefix) ? key.substring(tmpPrefix.length()) : "";
            if (fileName.isEmpty() || fileName.contains("/")) {
                throw new InvalidImageKeyException(i + 1, key);
            }
        }
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