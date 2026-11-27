package com.ject.chat.application.query;

import com.ject.chat.application.dto.PostChatRoomInfo;

/**
 * 다른 BC가 회원과 게시글에 대한 채팅 상태를 조회하는 application 공개 계약. 각 BC는 자기 application에 Port를 두고,
 * infrastructure Adapter에서 이 인터페이스를 호출한다. 구현은 채팅방 저장소(엔티티 · 마이그레이션)와 함께 추가한다.
 */
public interface PostChatRoomQuery {

    /**
     * 회원이 게시글로 만든 방에 대한 현재 채팅방 ID, 나간 이력, 유효 요청 수를 한 조회 시점 기준으로 함께 반환한다.
     * 반환된 세 값은 같은 조회 시점의 데이터이며, 이 메서드는 상태를 변경하거나 잠금을 획득하지 않는다.
     *
     * @param userId 조회를 요청한 인증 회원 ID. null이 아님을 전제한다
     * @param postId 조회할 게시글 ID. null이 아님을 전제한다
     * @return 같은 조회 시점의 현재 채팅방 ID · 나간 이력 · 유효 요청 수를 담은 {@link PostChatRoomInfo}
     */
    PostChatRoomInfo getPostChatRoomInfo(Long userId, Long postId);
}
