package com.ject.exchange.infrastructure.adapter;

import com.ject.exchange.application.dto.dto.PostInfo;
import com.ject.exchange.application.port.PostPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
public class PostAdapter implements PostPort {

    // Todo: post BC 조회 API가 생기면 실제 호출로 교체
    private final Map<Long, PostInfo> posts = Map.of(
            1L, new PostInfo(1L, "치이카와 우사기 가챠 피규어",
                    "https://{cdn}/post/1/1.jpg", 100L, PostInfo.Status.AVAILABLE),   // 100번 유저의 글
            2L, new PostInfo(2L, "다이소 빨래 detergent",
                    "https://{cdn}/post/2/1.jpg", 200L, PostInfo.Status.AVAILABLE),   // 200번 유저의 글
            3L, new PostInfo(3L, "스마트폰 거치대",
                    "https://{cdn}/post/3/1.jpg", 200L, PostInfo.Status.AVAILABLE),
            4L, new PostInfo(4L, "헤드폰",
                    "https://{cdn}/post/4/1.jpg", 200L, PostInfo.Status.RESERVED)     // 실패 케이스 확인용
    );

    @Override
    public Optional<PostInfo> findById(Long postId) {
        return Optional.ofNullable(posts.get(postId));
    }

    @Override
    public List<PostInfo> findAllByIds(List<Long> postIds) {
        return postIds.stream().map(posts::get).filter(Objects::nonNull).toList();
    }
}
