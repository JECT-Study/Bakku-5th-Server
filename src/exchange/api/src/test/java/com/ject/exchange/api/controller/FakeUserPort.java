package com.ject.exchange.api.controller;

import com.ject.exchange.application.dto.dto.UserInfo;
import com.ject.exchange.application.port.UserPort;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 컨트롤러 테스트용 가짜 회원 Port. UserPort 계약대로 없는 회원 id는 결과에서 빠지고 예외를 던지지 않는다.
 */
class FakeUserPort implements UserPort {

    private final Map<Long, UserInfo> users = new HashMap<>();

    @Override
    public List<UserInfo> findAllByIds(List<Long> userIds) {
        return userIds.stream()
                .map(users::get)
                .filter(Objects::nonNull)
                .toList();
    }

    void register(UserInfo user) {
        users.put(user.id(), user);
    }
}
