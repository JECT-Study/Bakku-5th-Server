package com.ject.notification.infrastructure.push;

import com.ject.common.event.notification.NotificationTargetType;
import com.ject.common.event.notification.NotificationType;
import com.ject.notification.application.dto.PushMessage;
import com.ject.notification.infrastructure.push.FcmClient.FcmResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FcmPushSenderTest {

    private static final PushMessage MESSAGE = new PushMessage(NotificationType.EXCHANGE_REQUESTED, "하치와레러버",
            "[포켓몬 카드] 교환 요청이 왔어요", NotificationTargetType.CHAT_ROOM, 5L);

    @Test
    @DisplayName("토큰이 500개를 넘으면 500개씩 나눠 보낸다")
    void splitBy500() {
        // given
        RecordingFcmClient fcmClient = new RecordingFcmClient();
        FcmPushSender sender = new FcmPushSender(fcmClient);
        List<String> tokens = IntStream.range(0, 1001).mapToObj(i -> "fcm-" + i).toList();

        // when
        sender.send(tokens, MESSAGE);

        // then
        assertEquals(List.of(500, 500, 1), fcmClient.requests.stream().map(List::size).toList());
        assertEquals("fcm-500", fcmClient.requests.get(1).getFirst());
        assertEquals("fcm-1000", fcmClient.requests.get(2).getFirst());
    }

    @Test
    @DisplayName("500개 이하면 한 번에 보낸다")
    void singleRequest() {
        // given
        RecordingFcmClient fcmClient = new RecordingFcmClient();
        FcmPushSender sender = new FcmPushSender(fcmClient);

        // when
        sender.send(List.of("fcm-1", "fcm-2"), MESSAGE);

        // then
        assertEquals(List.of(List.of("fcm-1", "fcm-2")), fcmClient.requests);
    }

    @Test
    @DisplayName("일부 토큰이 실패해도 예외 없이 끝난다 (실패는 로그로 남김)")
    void partialFailure() {
        // given
        FcmPushSender sender = new FcmPushSender((pushTokens, message) -> List.of(
                new FcmResult("fcm-1", FcmResult.Status.SENT, null, null),
                new FcmResult("fcm-2", FcmResult.Status.FAILED, "INTERNAL", "Internal error")));

        // when & then
        assertDoesNotThrow(() -> sender.send(List.of("fcm-1", "fcm-2"), MESSAGE));
    }

    @Test
    @DisplayName("나눠 보낸 요청들에서 더 쓸 수 없다고(UNREGISTERED) 답한 토큰만 모아 돌려준다")
    void collectUnregistered() {
        // given
        FcmPushSender sender = new FcmPushSender((pushTokens, message) -> pushTokens.stream()
                .map(token -> token.equals("fcm-3") || token.equals("fcm-600")
                        ? new FcmResult(token, FcmResult.Status.UNREGISTERED, "UNREGISTERED", "not found")
                        : token.equals("fcm-4")
                        ? new FcmResult(token, FcmResult.Status.FAILED, "INTERNAL", "Internal error")
                        : new FcmResult(token, FcmResult.Status.SENT, null, null))
                .toList());
        List<String> tokens = IntStream.range(0, 700).mapToObj(i -> "fcm-" + i).toList();

        // when
        List<String> unregistered = sender.send(tokens, MESSAGE);

        // then
        assertEquals(List.of("fcm-3", "fcm-600"), unregistered);
    }

    @Test
    @DisplayName("FCM 요청 자체가 실패하면 예외를 넘긴다 (알림 저장은 발송하는 쪽에서 남김)")
    void requestFailure() {
        // given
        FcmPushSender sender = new FcmPushSender((pushTokens, message) -> {
            throw new IllegalStateException("FCM 요청 실패");
        });

        // when & then
        assertThrows(IllegalStateException.class, () -> sender.send(List.of("fcm-1"), MESSAGE));
    }

    private static class RecordingFcmClient implements FcmClient {

        private final List<List<String>> requests = new ArrayList<>();

        @Override
        public List<FcmResult> send(List<String> pushTokens, PushMessage message) {
            requests.add(List.copyOf(pushTokens));
            return pushTokens.stream()
                    .map(token -> new FcmResult(token, FcmResult.Status.SENT, null, null))
                    .toList();
        }
    }
}
