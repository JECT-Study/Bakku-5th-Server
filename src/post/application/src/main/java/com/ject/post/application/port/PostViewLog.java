package com.ject.post.application.port;

import java.time.LocalDate;

public interface PostViewLog {

    boolean markViewed(Long postId, Long viewerId, LocalDate date);
}
