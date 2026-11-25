package com.ject.post.application;

import com.ject.post.domain.domain.Post;
import com.ject.post.domain.domain.PostStatus;
import com.ject.post.domain.repository.PostRepository;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FakePostRepository implements PostRepository {

    private final Map<Long, Post> posts = new HashMap<>();
    private final Instant createdAt;
    private long sequence;
    public int saveCalls;
    public int deleteCalls;
    public int updateStatusesCalls;
    public RuntimeException updateStatusesFailure;

    public FakePostRepository(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public Post save(Post post) {
        saveCalls++;
        Long id = post.getId() == null ? ++sequence : post.getId();
        Post saved = copy(post, id, post.getViewCount(),
                post.getCreatedAt() == null ? createdAt : post.getCreatedAt());
        posts.put(id, saved);
        return copy(saved);
    }

    @Override
    public void updateStatuses(List<Post> posts) {
        updateStatusesCalls++;
        if (updateStatusesFailure != null) {
            throw updateStatusesFailure;
        }
        posts.forEach(post -> this.posts.put(post.getId(), copy(post)));
    }

    @Override
    public void delete(Post post) {
        deleteCalls++;
        posts.put(post.getId(), copy(post));
    }

    @Override
    public int deleteAvailableByAuthor(Long authorId, Instant deletedAt) {
        List<Post> targets = posts.values().stream()
                .filter(post -> post.isWrittenBy(authorId) && post.getStatus() == PostStatus.AVAILABLE)
                .toList();
        targets.forEach(post -> posts.put(post.getId(), Post.builder()
                .id(post.getId())
                .authorId(post.getAuthorId())
                .status(PostStatus.DELETED)
                .imageKeys(post.getImageKeys())
                .tags(post.getTags())
                .title(post.getTitle())
                .category(post.getCategory())
                .opened(post.isOpened())
                .description(post.getDescription())
                .wantedItem(post.getWantedItem())
                .place(post.getPlace())
                .nowExchangeEndsAt(post.getNowExchangeEndsAt())
                .viewCount(post.getViewCount())
                .createdAt(post.getCreatedAt())
                .deletedAt(deletedAt)
                .build()));
        return targets.size();
    }

    @Override
    public Optional<Post> findById(Long id) {
        return Optional.ofNullable(posts.get(id)).map(FakePostRepository::copy);
    }

    @Override
    public void increaseViewCount(Long id) {
        Post post = posts.get(id);
        posts.put(id, copy(post, id, post.getViewCount() + 1, post.getCreatedAt()));
    }

    private static Post copy(Post post) {
        return copy(post, post.getId(), post.getViewCount(), post.getCreatedAt());
    }

    private static Post copy(Post post, Long id, int viewCount, Instant createdAt) {
        return Post.builder()
                .id(id)
                .authorId(post.getAuthorId())
                .status(post.getStatus())
                .imageKeys(post.getImageKeys())
                .tags(post.getTags())
                .title(post.getTitle())
                .category(post.getCategory())
                .opened(post.isOpened())
                .description(post.getDescription())
                .wantedItem(post.getWantedItem())
                .place(post.getPlace())
                .nowExchangeEndsAt(post.getNowExchangeEndsAt())
                .viewCount(viewCount)
                .createdAt(createdAt)
                .deletedAt(post.getDeletedAt())
                .build();
    }
}
