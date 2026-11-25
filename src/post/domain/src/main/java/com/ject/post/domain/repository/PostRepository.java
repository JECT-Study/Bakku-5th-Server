package com.ject.post.domain.repository;

import com.ject.post.domain.domain.Post;
import com.ject.post.domain.domain.PostStatus;
import com.ject.post.domain.exception.PostNotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PostRepository {
    Post save(Post post);
    Optional<Post> findById(Long id);
    void increaseViewCount(Long id);
    void delete(Post post);
    void updateStatuses(List<Post> posts);
    int deleteAvailableByAuthor(Long authorId, Instant deletedAt);

    default Post getVisible(Long id) {
        return findById(id)
                .filter(post -> post.getStatus() != PostStatus.DELETED)
                .orElseThrow(() -> new PostNotFoundException(id));
    }
}
