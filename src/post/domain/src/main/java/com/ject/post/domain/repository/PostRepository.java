package com.ject.post.domain.repository;

import com.ject.post.domain.domain.Post;
import com.ject.post.domain.domain.PostStatus;
import com.ject.post.domain.exception.PostNotFoundException;

import java.util.Optional;

public interface PostRepository {
    Post save(Post post);
    Optional<Post> findById(Long id);
    void increaseViewCount(Long id);
    void delete(Post post);

    default Post getVisible(Long id) {
        return findById(id)
                .filter(post -> post.getStatus() != PostStatus.DELETED)
                .orElseThrow(() -> new PostNotFoundException(id));
    }
}
