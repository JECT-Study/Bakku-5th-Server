package com.ject.notification.infrastructure.adapter;

import com.ject.notification.application.port.PushTokenCleanupPort;
import com.ject.user.application.command.PushTokenCommand;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class PushTokenCleanupAdapter implements PushTokenCleanupPort {

    private final PushTokenCommand pushTokenCommand;

    public PushTokenCleanupAdapter(PushTokenCommand pushTokenCommand) {
        this.pushTokenCommand = pushTokenCommand;
    }

    @Override
    public void deletePushTokens(Collection<String> pushTokens) {
        pushTokenCommand.deletePushTokens(pushTokens);
    }
}
