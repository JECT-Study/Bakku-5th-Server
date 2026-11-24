package com.ject.notification.infrastructure.push;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PushSenderConfigTest {

    private final PushSenderConfig config = new PushSenderConfig();

    @Test
    @DisplayName("키가 비어 있으면 로그만 남기는 발송기를 쓴다 (로컬 · 테스트)")
    void blankCredentials() {
        assertInstanceOf(LoggingPushSender.class, config.pushSender(""));
        assertInstanceOf(LoggingPushSender.class, config.pushSender("   "));
    }

    @Test
    @DisplayName("서비스 계정 키가 있으면 FCM 발송기를 만들고, 종료할 때 정리한다 (네트워크 없이 확인)")
    void validCredentials() throws Exception {
        // given: 테스트용으로 새로 만든 키 (실제 Firebase 키 아님)
        String encoded = Base64.getEncoder().encodeToString(fakeServiceAccountJson().getBytes(StandardCharsets.UTF_8));

        try {
            // when & then
            assertInstanceOf(FcmPushSender.class, config.pushSender(encoded));
        } finally {
            config.closeFirebase();
        }
        // 정리했으므로 같은 이름으로 다시 만들 수 있다 (재시작)
        PushSenderConfig restarted = new PushSenderConfig();
        try {
            assertInstanceOf(FcmPushSender.class, restarted.pushSender(encoded));
        } finally {
            restarted.closeFirebase();
        }
    }

    @Test
    @DisplayName("Base64가 아닌 값(JSON 그대로 등)을 넣으면 원인을 알려 주며 실패한다")
    void notBase64() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> PushSenderConfig.options("{\"type\": \"service_account\"}"));
        assertTrue(e.getMessage().contains("Base64"));
    }

    @Test
    @DisplayName("Base64지만 서비스 계정 키가 아니면 원인을 알려 주며 실패한다")
    void notServiceAccount() {
        String encoded = Base64.getEncoder().encodeToString("{\"hello\": 1}".getBytes(StandardCharsets.UTF_8));

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> PushSenderConfig.options(encoded));
        assertTrue(e.getMessage().contains("서비스 계정 키"));
    }

    private static String fakeServiceAccountJson() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        String pem = "-----BEGIN PRIVATE KEY-----\\n"
                + Base64.getEncoder().encodeToString(generator.generateKeyPair().getPrivate().getEncoded())
                + "\\n-----END PRIVATE KEY-----\\n";
        return """
                {"type": "service_account", "project_id": "test-project", "private_key_id": "test",
                 "private_key": "%s", "client_email": "test@test-project.iam.gserviceaccount.com",
                 "client_id": "1", "token_uri": "https://oauth2.googleapis.com/token"}
                """.formatted(pem);
    }
}
