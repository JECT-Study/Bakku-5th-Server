package com.ject.notification.application;

import com.ject.notification.application.dto.PushMessage;
import com.ject.notification.application.port.PushSender;

import java.util.ArrayList;
import java.util.List;

class FakePushSender implements PushSender {

    record Sent(List<String> pushTokens, PushMessage message) {
    }

    private final List<Sent> sent = new ArrayList<>();
    private List<String> unusableTokens = List.of();

    @Override
    public List<String> send(List<String> pushTokens, PushMessage message) {
        sent.add(new Sent(List.copyOf(pushTokens), message));
        return pushTokens.stream().filter(unusableTokens::contains).toList();
    }

    /** 이 토큰들은 FCM이 더 쓸 수 없다고 답한 것으로 돌려준다 */
    void markUnusable(List<String> pushTokens) {
        unusableTokens = List.copyOf(pushTokens);
    }

    List<Sent> sent() {
        return List.copyOf(sent);
    }
}
