package com.ject.notification.application;

import com.ject.notification.application.dto.PushMessage;
import com.ject.notification.application.port.PushSender;

import java.util.ArrayList;
import java.util.List;

class FakePushSender implements PushSender {

    record Sent(List<String> pushTokens, PushMessage message) {
    }

    private final List<Sent> sent = new ArrayList<>();

    @Override
    public void send(List<String> pushTokens, PushMessage message) {
        sent.add(new Sent(List.copyOf(pushTokens), message));
    }

    List<Sent> sent() {
        return List.copyOf(sent);
    }
}
