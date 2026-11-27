package com.ject.exchange.application.port;

import java.util.List;

public interface ImagePort {
    /**
     * 새로 작성한 아이템 사진을 이 교환의 정식 경로로 확정하고, 확정된 key를 같은 순서로 돌려준다.
     * 이미 이 교환의 정식 key는 그대로 돌려준다. 쓸 수 없는 key가 있으면 {@link ExchangeImageRejectedException}
     *
     * @param uploaderId 사진을 올린 회원 (교환 요청자)
     */
    List<String> attachToExchange(Long uploaderId, Long exchangeId, List<String> imageKeys);

    /**
     * 채팅방 생성 실패 등으로 교환이 끝내 저장되지 않을 때, 이미 이 교환의 정식 경로로 확정한 이미지 객체를 저장소에서
     * 지운다(보상 삭제). DB 트랜잭션 롤백은 저장소의 파일 이동까지 되돌리지 않으므로 별도로 지워야 한다
     */
    void discardFromExchange(Long exchangeId, List<String> imageKeys);
}
