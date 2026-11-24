package com.ject.notification.infrastructure.push;

import com.ject.notification.application.dto.PushMessage;

import java.util.List;

/**
 * FCM에 한 번 요청한다. Firebase SDK를 감싸 나누기 · 결과 처리를 SDK 없이 확인할 수 있게 한다.
 */
interface FcmClient {

    /** FCM 한 번 요청에 담을 수 있는 최대 토큰 수 */
    int MAX_TOKENS_PER_REQUEST = 500;

    /**
     * @param pushTokens 최대 {@link #MAX_TOKENS_PER_REQUEST}개
     * @return 토큰마다 결과, pushTokens와 같은 순서
     */
    List<FcmResult> send(List<String> pushTokens, PushMessage message);

    /**
     * @param errorCode    실패 이유(FCM 오류 코드), 성공이면 null
     * @param errorMessage FCM이 준 실패 설명(토큰은 들어 있지 않음), 성공이면 null
     */
    record FcmResult(String pushToken, Status status, String errorCode, String errorMessage) {

        enum Status {
            SENT,
            /** 앱 삭제 등으로 더 쓸 수 없는 토큰 */
            UNREGISTERED,
            FAILED
        }
    }
}
