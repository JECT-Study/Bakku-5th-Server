package com.ject.post.domain.repository;

import com.ject.post.domain.domain.Post;

import java.util.Optional;

public interface PostRepository {
    Post save(Post post);
    Optional<Post> findById(Long id);
}
