package com.ject.notification.infrastructure.push;

import com.ject.notification.application.dto.PushMessage;
import com.ject.notification.application.port.PushSender;
import com.ject.notification.infrastructure.push.FcmClient.FcmResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * FCM으로 보낸다. 토큰이 많으면 한 번 요청 한도(500개)씩 나눠 보낸다.
 * 토큰은 개인 기기 식별값이라 로그에는 개수 · 오류 코드만 남긴다.
 */
public class FcmPushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(FcmPushSender.class);

    private final FcmClient fcmClient;

    FcmPushSender(FcmClient fcmClient) {
        this.fcmClient = fcmClient;
    }

    @Override
    public void send(List<String> pushTokens, PushMessage message) {
        for (int from = 0; from < pushTokens.size(); from += FcmClient.MAX_TOKENS_PER_REQUEST) {
            List<String> chunk = pushTokens.subList(from,
                    Math.min(from + FcmClient.MAX_TOKENS_PER_REQUEST, pushTokens.size()));
            logFailures(message, fcmClient.send(chunk, message));
        }
    }

    /**
     * 실패를 오류 코드별 개수와 첫 실패 설명으로 남긴다 (예: {UNREGISTERED=2, THIRD_PARTY_AUTH_ERROR=1}).
     * THIRD_PARTY_AUTH_ERROR가 계속 나오면 Firebase에 올린 APNs 키를 확인한다.
     */
    private static void logFailures(PushMessage message, List<FcmResult> results) {
        List<FcmResult> failed = results.stream()
                .filter(result -> result.status() != FcmResult.Status.SENT)
                .toList();
        if (failed.isEmpty()) {
            return;
        }
        Map<String, Long> failedByCode = failed.stream()
                .collect(Collectors.groupingBy(result -> String.valueOf(result.errorCode()), TreeMap::new,
                        Collectors.counting()));
        log.warn("푸시 일부 실패. type={}, tokens={}, failed={}, firstError={}", message.type(), results.size(),
                failedByCode, failed.getFirst().errorMessage());
    }
}
