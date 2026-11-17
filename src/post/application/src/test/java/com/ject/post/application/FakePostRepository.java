package com.ject.post.application;

import com.ject.post.domain.domain.Post;
import com.ject.post.domain.repository.PostRepository;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FakePostRepository implements PostRepository {

    private final Map<Long, Post> posts = new HashMap<>();
    private final Instant createdAt;
    private long sequence;

    public FakePostRepository(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public Post save(Post post) {
        Long id = post.getId() == null ? ++sequence : post.getId();
        Post saved = Post.builder()
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
                .viewCount(post.getViewCount())
                .createdAt(post.getCreatedAt() == null ? createdAt : post.getCreatedAt())
                .build();
        posts.put(id, saved);
        return saved;
    }

    @Override
    public Optional<Post> findById(Long id) {
        return Optional.ofNullable(posts.get(id));
    }

    @Override
    public void increaseViewCount(Long id) {
        Post post = posts.get(id);
        posts.put(id, Post.builder()
                .id(post.getId())
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
                .viewCount(post.getViewCount() + 1)
                .createdAt(post.getCreatedAt())
                .build());
    }
}
