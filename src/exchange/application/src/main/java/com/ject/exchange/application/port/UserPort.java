package com.ject.exchange.application.port;

import com.ject.exchange.application.dto.dto.UserInfo;

import java.util.List;

public interface UserPort {
    List<UserInfo> findAllByIds(List<Long> userIds);
}
