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
}
