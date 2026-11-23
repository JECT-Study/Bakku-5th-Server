package com.ject.exchange.application.port;

import com.ject.exchange.application.dto.dto.PostInfo;

import java.util.List;
import java.util.Optional;

public interface PostPort {
    /**
     * 글 하나를 조회한다. 삭제된 글은 status DELETED로 들어 있고, 없는 글은 empty
     */
    Optional<PostInfo> findById(Long postId);

    /**
     * 글들을 조회한다. 삭제된 글은 status DELETED로 포함하고, 없는 ID는 빠진다
     */
    List<PostInfo> findAllByIds(List<Long> postIds);

    /**
     * 글들을 예약중으로 바꾸도록 post BC에 요청한다 (약속 확정 시 교환글과 고른 첨부 글).
     * 하나라도 바꿀 수 없으면 {@link PostStatusRejectedException}을 던지고 아무것도 바뀌지 않는다 (호출한 트랜잭션 롤백)
     */
    void reserve(List<Long> postIds);

    /**
     * 글들을 교환 완료로 바꾸도록 post BC에 요청한다 (교환 확인에서 교환했어요, 교환글과 약속에서 고른 첨부 글).
     * 하나라도 바꿀 수 없으면 {@link PostStatusRejectedException}을 던지고 아무것도 바뀌지 않는다 (호출한 트랜잭션 롤백)
     */
    void complete(List<Long> postIds);

    /**
     * 글들의 예약을 해제해 교환 가능으로 되돌리도록 post BC에 요청한다 (교환 확인에서 못 만났어요).
     * 하나라도 바꿀 수 없으면 {@link PostStatusRejectedException}을 던지고 아무것도 바뀌지 않는다 (호출한 트랜잭션 롤백)
     */
    void release(List<Long> postIds);
}
