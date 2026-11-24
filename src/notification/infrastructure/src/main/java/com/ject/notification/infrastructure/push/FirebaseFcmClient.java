package com.ject.notification.infrastructure.push;

import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import com.ject.notification.application.dto.PushMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Firebase Admin SDK로 보낸다. 앱이 알림을 눌렀을 때 이동할 수 있게 종류 · 대상을 data에 같이 담는다.
 */
class FirebaseFcmClient implements FcmClient {

    static final String DATA_TYPE = "type";
    static final String DATA_TARGET_TYPE = "targetType";
    static final String DATA_TARGET_ID = "targetId";

    private final FirebaseMessaging firebaseMessaging;

    FirebaseFcmClient(FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
    }

    @Override
    public List<FcmResult> send(List<String> pushTokens, PushMessage message) {
        BatchResponse response;
        try {
            response = firebaseMessaging.sendEachForMulticast(toMulticast(pushTokens, message));
        } catch (FirebaseMessagingException e) {
            throw new IllegalStateException("FCM 요청 실패. code=" + e.getMessagingErrorCode(), e);
        }
        List<SendResponse> responses = response.getResponses();
        List<FcmResult> results = new ArrayList<>(responses.size());
        for (int i = 0; i < responses.size(); i++) {
            results.add(toResult(pushTokens.get(i), responses.get(i)));
        }
        return results;
    }

    private static MulticastMessage toMulticast(List<String> pushTokens, PushMessage message) {
        return MulticastMessage.builder()
                .addAllTokens(pushTokens)
                .setNotification(Notification.builder()
                        .setTitle(message.title())
                        .setBody(message.body())
                        .build())
                .putAllData(data(message))
                .setApnsConfig(ApnsConfig.builder()
                        .setAps(Aps.builder().setSound("default").build())
                        .build())
                .build();
    }

    /**
     * 앱이 알림을 눌렀을 때 쓰는 값. FCM data 값은 null을 넣을 수 없어 대상이 없으면 키를 뺀다.
     */
    static Map<String, String> data(PushMessage message) {
        Map<String, String> data = new HashMap<>();
        data.put(DATA_TYPE, message.type().name());
        if (message.targetType() != null) {
            data.put(DATA_TARGET_TYPE, message.targetType().name());
        }
        if (message.targetId() != null) {
            data.put(DATA_TARGET_ID, message.targetId().toString());
        }
        return data;
    }

    private static FcmResult toResult(String pushToken, SendResponse response) {
        if (response.isSuccessful()) {
            return new FcmResult(pushToken, FcmResult.Status.SENT, null, null);
        }
        FirebaseMessagingException exception = response.getException();
        MessagingErrorCode messagingErrorCode = exception.getMessagingErrorCode();
        FcmResult.Status status = messagingErrorCode == MessagingErrorCode.UNREGISTERED
                ? FcmResult.Status.UNREGISTERED
                : FcmResult.Status.FAILED;
        return new FcmResult(pushToken, status, errorCode(exception), exception.getMessage());
    }

    /**
     * FCM 전용 코드가 없으면(인증 · 권한 문제 등) 공통 오류 코드를 쓴다.
     */
    private static String errorCode(FirebaseMessagingException exception) {
        if (exception.getMessagingErrorCode() != null) {
            return exception.getMessagingErrorCode().name();
        }
        return String.valueOf(exception.getErrorCode());
    }
}
