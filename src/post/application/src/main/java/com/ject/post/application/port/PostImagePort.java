package com.ject.post.application.port;

import java.util.List;

public interface PostImagePort {

    List<String> attachToPost(Long authorId, Long postId, List<String> imageKeys);
}
