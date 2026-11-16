package com.ject.exchange.application.port;

import com.ject.exchange.application.dto.dto.PostInfo;

import java.util.List;
import java.util.Optional;

public interface PostPort {
    /**
     * 게시글 ID로 게시글 정보를 조회합니다.
     * @param postId
     * @return
     */
    Optional<PostInfo> findById(Long postId);
    /**
     * 게시글 ID 목록으로 게시글 정보를 조회합니다.
     * @param postIds
     * @return
     */
    List<PostInfo> findAllByIds(List<Long> postIds);
}
