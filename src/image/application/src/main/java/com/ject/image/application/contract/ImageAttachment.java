package com.ject.image.application.contract;

import java.util.List;

public interface ImageAttachment {

    /**
     * 업로드한 tmp 이미지를 대상(글 · 교환 · 채팅) 경로로 확정하고, 확정된 key를 같은 순서로 돌려준다.
     * 이미 그 대상의 정식 key는 그대로 돌려준다.
     *
     * @param purpose    POST, EXCHANGE, CHAT
     * @param uploaderId 이미지를 올린 회원
     * @param targetId   글 · 교환 · 채팅 ID
     * @throws com.ject.image.application.exception.ImageKeyRejectedException 용도 · 올린 회원 · 대상이 맞지 않거나 저장소에 없는 key
     */
    List<String> finalizeImages(String purpose, Long uploaderId, Long targetId, List<String> keys);

    /**
     * 대상에서 빠진 정식 이미지를 저장소에서 지운다. 그 대상의 정식 key가 아니면 건너뛴다.
     * DB 커밋 후에 부른다.
     *
     * @param purpose  POST, EXCHANGE, CHAT
     * @param targetId 글 · 교환 · 채팅 ID
     */
    void discardImages(String purpose, Long targetId, List<String> keys);
}
