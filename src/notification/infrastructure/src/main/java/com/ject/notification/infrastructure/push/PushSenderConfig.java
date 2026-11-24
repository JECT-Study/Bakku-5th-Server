package com.ject.notification.infrastructure.push;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.ject.notification.application.port.PushSender;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Base64;

/**
 * Firebase 서비스 계정 키가 있으면 FCM으로 보내고, 없으면(로컬 · 테스트) 로그만 남긴다.
 * 키가 있는데 읽을 수 없으면 앱을 띄우지 않는다. 푸시가 조용히 안 나가는 것보다 배포 때 바로 드러나는 편이 낫다.
 */
@Configuration
public class PushSenderConfig {

    private static final Logger log = LoggerFactory.getLogger(PushSenderConfig.class);

    /** 다른 곳에서 기본 FirebaseApp을 만들어도 겹치지 않게 이름을 붙인다 */
    private static final String FIREBASE_APP_NAME = "bakku-push";

    /** FCM이 응답하지 않을 때 알림 스레드 · DB 연결을 오래 잡지 않게 한다 */
    private static final int TIMEOUT_MILLIS = (int) Duration.ofSeconds(10).toMillis();

    private FirebaseApp firebaseApp;

    /**
     * @param credentials 서비스 계정 키 JSON을 Base64로 인코딩한 값 (환경변수 FCM_CREDENTIALS)
     */
    @Bean
    public PushSender pushSender(@Value("${app.fcm.credentials:}") String credentials) {
        if (credentials.isBlank()) {
            log.warn("FCM 서비스 계정 키가 없어 푸시를 보내지 않고 로그만 남깁니다.");
            return new LoggingPushSender();
        }
        firebaseApp = FirebaseApp.initializeApp(options(credentials), FIREBASE_APP_NAME);
        log.info("FCM 푸시 발송을 켭니다.");
        return new FcmPushSender(new FirebaseFcmClient(FirebaseMessaging.getInstance(firebaseApp)));
    }

    /**
     * 종료할 때 Firebase 연결 · 스레드를 정리한다. 같은 프로세스에서 다시 띄워도 이름이 겹치지 않는다.
     */
    @PreDestroy
    void closeFirebase() {
        if (firebaseApp != null) {
            firebaseApp.delete();
        }
    }

    static FirebaseOptions options(String credentials) {
        byte[] json;
        try {
            json = Base64.getDecoder().decode(credentials.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("FCM_CREDENTIALS는 서비스 계정 키 JSON 파일을 Base64로 인코딩한 값이어야 합니다.", e);
        }
        try {
            // SDK 기본 통신(Apache HttpClient 5)은 스프링이 맞춘 버전과 달라 FCM 오류 응답 본문을 읽지 못한다
            // (UNREGISTERED를 알아보지 못해 토큰 정리가 안 됨). 자바 기본 HTTP 통신을 쓴다
            return FirebaseOptions.builder()
                    .setHttpTransport(new NetHttpTransport())
                    .setCredentials(GoogleCredentials.fromStream(new ByteArrayInputStream(json)))
                    .setConnectTimeout(TIMEOUT_MILLIS)
                    .setReadTimeout(TIMEOUT_MILLIS)
                    .build();
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("FCM_CREDENTIALS를 서비스 계정 키로 읽지 못했습니다. Firebase 콘솔에서 받은 JSON인지 확인하세요.", e);
        }
    }
}
