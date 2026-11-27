package com.ject.exchange.application.port;

import com.ject.exchange.application.dto.dto.ChatRoomInfo;

import java.util.Optional;

/**
 * 교환 요청 생성 흐름에서 chat BC의 채팅방을 찾거나 만드는 Port
 */
public interface ChatRoomPort {

    /**
     * (postId, requesterId) 조합의 잠금을 먼저 잡고, 그 안에서 기존 연결 상태를 조회하도록 chat BC에 요청한다.
     * 호출한 이 트랜잭션이 끝날 때까지 잠금이 유지된다
     */
    ChatRoomInfo lockAndFind(Long postId, Long requesterId);

    /**
     * 신규 교환 저장 뒤 채팅방과 요청자·게시자 참여자를 만들도록 chat BC에 요청한다
     */
    Long create(Long exchangeId, Long postId, Long requesterId, Long ownerId);

    /**
     * 교환에 연결된 채팅방 ID를 조회한다. 아직 없으면 empty
     */
    Optional<Long> findChatRoomIdByExchangeId(Long exchangeId);
}
