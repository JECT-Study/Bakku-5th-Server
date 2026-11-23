package com.ject.notification.infrastructure.adapter;

import com.ject.notification.application.port.PushTokenPort;
import com.ject.user.application.query.PushTokenQuery;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
public class PushTokenAdapter implements PushTokenPort {

    private final PushTokenQuery pushTokenQuery;

    public PushTokenAdapter(PushTokenQuery pushTokenQuery) {
        this.pushTokenQuery = pushTokenQuery;
    }

    @Override
    public List<String> getPushTokens(Collection<Long> memberIds) {
        return pushTokenQuery.getPushTokens(memberIds);
    }
}
