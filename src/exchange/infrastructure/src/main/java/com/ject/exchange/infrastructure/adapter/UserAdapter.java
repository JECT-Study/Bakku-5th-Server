package com.ject.exchange.infrastructure.adapter;

import com.ject.exchange.application.dto.dto.UserInfo;
import com.ject.exchange.application.port.UserPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class UserAdapter implements UserPort {

    // Todo: user, reputation BC 조회 API가 생기면 실제 호출로 교체
    private final Map<Long, UserInfo> users = Map.of(
            100L, new UserInfo(100L, "치이카와러버"),
            200L, new UserInfo(200L, "하치와레러버")
    );

    @Override
    public List<UserInfo> findAllByIds(List<Long> userIds) {
        return userIds.stream().map(users::get).filter(Objects::nonNull).toList();
    }
}
