package com.ject.post.application;

import com.ject.post.application.port.PostViewLog;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

public class FakePostViewLog implements PostViewLog {

    private final Set<String> viewed = new HashSet<>();

    @Override
    public boolean markViewed(Long postId, Long viewerId, LocalDate date) {
        return viewed.add(postId + ":" + viewerId + ":" + date);
    }
}
