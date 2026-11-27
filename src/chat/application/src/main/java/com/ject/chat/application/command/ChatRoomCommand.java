package com.ject.chat.application.command;

import com.ject.chat.application.dto.ChatRoomLink;

/**
 * 다른 BC(exchange 등)가 교환 요청 생성 흐름에서 채팅방을 찾거나 만드는 application 공개 계약. 각 BC는 자기 application에
 * Port를 두고, infrastructure Adapter에서 이 인터페이스를 호출한다. 구현은 채팅방 저장소(엔티티 · 마이그레이션)와 함께 추가한다.
 */
public interface ChatRoomCommand {

    /**
     * (postId, requesterId) 조합의 advisory transaction lock을 먼저 잡고, 그 안에서 기존 연결 상태를 조회해 반환한다.
     * 호출한 외부 트랜잭션이 끝날 때까지 이 잠금이 유지된다고 전제한다.
     *
     * @param postId      요청 대상 게시글 ID. null이 아님을 전제한다
     * @param requesterId 요청자 ID. null이 아님을 전제한다
     * @return 동일 조합의 현재 연결 상태
     */
    ChatRoomLink lockAndFind(Long postId, Long requesterId);

    /**
     * 신규 교환 저장 뒤 채팅방과 요청자·게시자 참여자를 만든다.
     *
     * @param exchangeId  새로 저장된 교환 ID. null이 아님을 전제한다
     * @param postId      요청 대상 게시글 ID. null이 아님을 전제한다
     * @param requesterId 요청자 ID. null이 아님을 전제한다
     * @param ownerId     게시자 ID. null이 아님을 전제한다
     * @return 생성된 채팅방 ID
     */
    Long create(Long exchangeId, Long postId, Long requesterId, Long ownerId);
}
