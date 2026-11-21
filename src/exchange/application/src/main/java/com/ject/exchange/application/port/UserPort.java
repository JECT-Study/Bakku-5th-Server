package com.ject.exchange.application.port;

import com.ject.exchange.application.dto.dto.UserInfo;

import java.util.List;

public interface UserPort {
    /**
     * 여러 회원의 정보를 한 번에 조회한다.
     * 없는 회원 ID는 결과에서 빠진다 (예외를 던지지 않는다). 순서는 보장하지 않는다
     */
    List<UserInfo> findAllByIds(List<Long> userIds);
}
