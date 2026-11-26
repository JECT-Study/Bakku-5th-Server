package com.ject.post.application.service.command;

import com.ject.common.core.time.ServiceTime;
import com.ject.post.domain.domain.Post;
import com.ject.post.domain.domain.PostStatus;
import com.ject.post.domain.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WithdrawnMemberReservedPostService {

    private final PostRepository postRepository;
    private final ServiceTime serviceTime;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void settle(List<Long> postIds, Long withdrawnMemberId) {
        Instant now = serviceTime.now();
        List<Post> released = new ArrayList<>();
        for (Long postId : postIds) {
            Optional<Post> found = postRepository.findById(postId);
            if (found.isEmpty()) {
                continue;
            }
            Post post = found.get();
            if (post.isWrittenBy(withdrawnMemberId)) {
                if (post.deleteByAuthorWithdrawal(now)) {
                    postRepository.delete(post);
                }
            } else if (post.getStatus() == PostStatus.RESERVED) {
                post.release();
                released.add(post);
            }
        }
        if (!released.isEmpty()) {
            postRepository.updateStatuses(released);
        }
    }
}
