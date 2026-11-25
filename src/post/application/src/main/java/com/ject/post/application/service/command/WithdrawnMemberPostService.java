package com.ject.post.application.service.command;

import com.ject.post.domain.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class WithdrawnMemberPostService {

    private final PostRepository postRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int deleteAvailablePosts(Long memberId, Instant withdrawnAt) {
        return postRepository.deleteAvailableByAuthor(memberId, withdrawnAt);
    }
}
