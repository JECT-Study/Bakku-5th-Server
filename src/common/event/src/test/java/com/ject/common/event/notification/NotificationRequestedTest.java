package com.ject.common.event.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NotificationRequestedTest {

    @Test
    @DisplayName("문구 값이 없으면 빈 값으로 처리한다")
    void nullParamsBecomeEmpty() {
        // when
        NotificationRequested event = new NotificationRequested(NotificationType.CHAT_MESSAGE, List.of(1L),
                2L, null, null, null, null);

        // then
        assertEquals(Map.of(), event.params());
    }

    @Test
    @DisplayName("발행 뒤 원본 목록이 바뀌어도 받을 사람은 바뀌지 않는다")
    void copiesReceivers() {
        // given
        List<Long> receivers = new ArrayList<>(List.of(1L));
        NotificationRequested event = new NotificationRequested(NotificationType.CHAT_MESSAGE, receivers,
                2L, null, null, null, null);

        // when
        receivers.add(3L);

        // then
        assertEquals(List.of(1L), event.receiverIds());
    }

    @Test
    @DisplayName("받을 사람 목록은 비워 둘 수 없다")
    void requiresReceivers() {
        assertThrows(NullPointerException.class, () -> new NotificationRequested(NotificationType.CHAT_MESSAGE, null,
                2L, null, null, null, null));
    }
}
