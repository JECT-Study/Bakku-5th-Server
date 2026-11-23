package com.ject.exchange.infrastructure.adapter;

import com.ject.exchange.application.dto.dto.PostInfo;
import com.ject.exchange.application.port.PostPort;
import com.ject.exchange.application.port.PostStatusRejectedException;
import com.ject.post.application.contract.PostBrief;
import com.ject.post.application.contract.PostLookup;
import com.ject.post.application.contract.PostStatusChangeRejectedException;
import com.ject.post.application.contract.PostStatusChanger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class PostAdapter implements PostPort {

    private final PostLookup postLookup;
    private final PostStatusChanger postStatusChanger;

    @Override
    public Optional<PostInfo> findById(Long postId) {
        return findAllByIds(List.of(postId)).stream().findFirst();
    }

    @Override
    public List<PostInfo> findAllByIds(List<Long> postIds) {
        if (postIds.isEmpty()) {
            return List.of();
        }
        return postLookup.findAllByIds(postIds).stream()
                .map(PostAdapter::toPostInfo)
                .toList();
    }

    @Override
    public void reserve(List<Long> postIds) {
        changeStatus(postIds, postStatusChanger::reserve);
    }

    @Override
    public void complete(List<Long> postIds) {
        changeStatus(postIds, postStatusChanger::complete);
    }

    @Override
    public void release(List<Long> postIds) {
        changeStatus(postIds, postStatusChanger::release);
    }

    private static void changeStatus(List<Long> postIds, Consumer<List<Long>> change) {
        try {
            change.accept(postIds);
        } catch (PostStatusChangeRejectedException rejected) {
            throw new PostStatusRejectedException(rejected.getPostId());
        }
    }

    private static PostInfo toPostInfo(PostBrief brief) {
        return new PostInfo(brief.id(), brief.title(), brief.thumbnailKey(), brief.authorId(), statusOf(brief));
    }

    /**
     * 모르는 상태를 임의 상태로 보면 요청 · 약속 판정이 틀어지므로 실패시킨다
     */
    private static PostInfo.Status statusOf(PostBrief brief) {
        return Arrays.stream(PostInfo.Status.values())
                .filter(status -> status.name().equals(brief.status()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "알 수 없는 글 상태입니다. postId=" + brief.id() + " status=" + brief.status()));
    }
}
