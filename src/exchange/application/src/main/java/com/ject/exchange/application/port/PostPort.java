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

    /**
     * 글들을 예약중으로 바꾸도록 post BC에 요청한다 (약속 확정 시 교환글과 고른 첨부 글).
     * 실패하면 예외를 던지고, 호출한 트랜잭션은 롤백된다
     */
    void reserve(List<Long> postIds);
}
