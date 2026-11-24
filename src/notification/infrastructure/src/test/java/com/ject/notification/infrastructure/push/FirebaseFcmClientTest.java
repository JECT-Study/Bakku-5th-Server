package com.ject.notification.infrastructure.push;

import com.ject.common.event.notification.NotificationTargetType;
import com.ject.common.event.notification.NotificationType;
import com.ject.notification.application.dto.PushMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FirebaseFcmClientTest {

    @Test
    @DisplayName("앱이 눌렀을 때 이동할 수 있게 종류 · 대상 종류 · 대상 ID를 문자열로 담는다")
    void dataWithTarget() {
        // given
        PushMessage message = new PushMessage(NotificationType.EXCHANGE_REQUESTED, "하치와레러버",
                "[포켓몬 카드] 교환 요청이 왔어요", NotificationTargetType.CHAT_ROOM, 5L);

        // when & then
        assertEquals(Map.of("type", "EXCHANGE_REQUESTED", "targetType", "CHAT_ROOM", "targetId", "5"),
                FirebaseFcmClient.data(message));
    }

    @Test
    @DisplayName("이동할 대상이 없으면 대상 키를 빼고 종류만 담는다 (FCM data에는 null을 넣을 수 없음)")
    void dataWithoutTarget() {
        // given
        PushMessage message = new PushMessage(NotificationType.AUTO_COMPLETE_SOON, "치이카와",
                "내일 교환이 자동으로 완료돼요", null, null);

        // when & then
        assertEquals(Map.of("type", "AUTO_COMPLETE_SOON"), FirebaseFcmClient.data(message));
    }
}
