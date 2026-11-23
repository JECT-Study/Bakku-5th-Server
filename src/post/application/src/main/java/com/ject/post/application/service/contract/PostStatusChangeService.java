package com.ject.post.application.service.contract;

import com.ject.post.application.contract.PostStatusChangeRejectedException;
import com.ject.post.application.contract.PostStatusChangeRejectedException.Reason;
import com.ject.post.application.contract.PostStatusChanger;
import com.ject.post.domain.domain.Post;
import com.ject.post.domain.exception.PostConflictException;
import com.ject.post.domain.exception.PostNotFoundException;
import com.ject.post.domain.exception.PostStatusTransitionException;
import com.ject.post.domain.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class PostStatusChangeService implements PostStatusChanger {

    private final PostRepository postRepository;

    @Override
    @Transactional
    public void reserve(List<Long> postIds) {
        change(postIds, Post::reserve);
    }

    @Override
    @Transactional
    public void release(List<Long> postIds) {
        change(postIds, Post::release);
    }

    @Override
    @Transactional
    public void complete(List<Long> postIds) {
        change(postIds, Post::complete);
    }

    private void change(List<Long> postIds, Consumer<Post> transition) {
        List<Post> posts = new ArrayList<>();
        for (Long postId : new LinkedHashSet<>(postIds)) {
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new PostStatusChangeRejectedException(postId, Reason.NOT_FOUND, null));
            try {
                transition.accept(post);
            } catch (PostNotFoundException e) {
                throw new PostStatusChangeRejectedException(postId, Reason.NOT_FOUND, e);
            } catch (PostStatusTransitionException e) {
                throw new PostStatusChangeRejectedException(postId, Reason.INVALID_STATUS, e);
            }
            posts.add(post);
        }
        if (posts.isEmpty()) {
            return;
        }
        try {
            postRepository.updateStatuses(posts);
        } catch (PostConflictException e) {
            throw new PostStatusChangeRejectedException(e.getPostId(), Reason.CONFLICT, e);
        }
    }
}
