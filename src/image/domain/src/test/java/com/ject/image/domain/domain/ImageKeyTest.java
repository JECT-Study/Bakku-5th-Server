package com.ject.image.domain.domain;

import com.ject.image.domain.exception.ImageNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageKeyTest {

    private static final String FILE = "8f14e45f-ceea-467f-a8f5-6e8e6e5c7d1a.jpg";

    @Test
    @DisplayName("새 tmp key는 {용도}/tmp/{회원}/{UUID}.{확장자}이다")
    void newTmp() {
        ImageKey key = ImageKey.newTmp(ImagePurpose.CHAT, 3L, ImageFormat.WEBP);

        assertTrue(key.isTmp());
        assertTrue(key.value().matches("chat/tmp/3/[0-9a-f-]{36}\\.webp"), key.value());
        assertEquals(key, ImageKey.parse(key.value()));
    }

    @Test
    @DisplayName("tmp key를 해석한다")
    void parse_tmp() {
        ImageKey key = ImageKey.parse("post/tmp/3/" + FILE);

        assertEquals(ImagePurpose.POST, key.purpose());
        assertEquals(3L, key.uploaderId());
        assertNull(key.targetId());
        assertEquals(FILE, key.fileName());
        assertEquals(ImageFormat.JPEG, key.format());
    }

    @Test
    @DisplayName("정식 key를 해석한다")
    void parse_final() {
        ImageKey key = ImageKey.parse("exchange/10/" + FILE);

        assertFalse(key.isTmp());
        assertEquals(ImagePurpose.EXCHANGE, key.purpose());
        assertEquals(10L, key.targetId());
        assertEquals("exchange/10/" + FILE, key.value());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "post/10/8f14e45f.jpg",
            "user/10/8f14e45f-ceea-467f-a8f5-6e8e6e5c7d1a.jpg",
            "post/10/8f14e45f-ceea-467f-a8f5-6e8e6e5c7d1a.gif",
            "post/../10/8f14e45f-ceea-467f-a8f5-6e8e6e5c7d1a.jpg",
            "post/tmp/abc/8f14e45f-ceea-467f-a8f5-6e8e6e5c7d1a.jpg",
            "/post/10/8f14e45f-ceea-467f-a8f5-6e8e6e5c7d1a.jpg"
    })
    @DisplayName("형식이 맞지 않는 key는 IMAGE_002")
    void parse_invalid(String value) {
        assertThrows(ImageNotFoundException.class, () -> ImageKey.parse(value));
    }

    @Test
    @DisplayName("tmp key를 대상 경로로 옮기면 파일 이름은 그대로다")
    void toFinal() {
        ImageKey finalKey = ImageKey.parse("post/tmp/3/" + FILE).toFinal(10L);

        assertEquals("post/10/" + FILE, finalKey.value());
    }

    @Test
    @DisplayName("정식 key는 다시 옮길 수 없다")
    void toFinal_alreadyFinal() {
        ImageKey key = ImageKey.parse("post/10/" + FILE);

        assertThrows(IllegalStateException.class, () -> key.toFinal(11L));
    }

    @Test
    @DisplayName("tmp 이미지는 올린 회원만 볼 수 있다")
    void canBeViewedBy_tmp() {
        ImageKey key = ImageKey.parse("post/tmp/3/" + FILE);

        assertTrue(key.canBeViewedBy(3L));
        assertFalse(key.canBeViewedBy(4L));
    }

    @Test
    @DisplayName("교환글 정식 이미지는 누구나 볼 수 있다")
    void canBeViewedBy_postFinal() {
        assertTrue(ImageKey.parse("post/10/" + FILE).canBeViewedBy(4L));
    }

    @Test
    @DisplayName("교환 · 채팅 정식 이미지는 아직 볼 수 없다")
    void canBeViewedBy_exchangeAndChatFinal() {
        assertFalse(ImageKey.parse("exchange/10/" + FILE).canBeViewedBy(3L));
        assertFalse(ImageKey.parse("chat/10/" + FILE).canBeViewedBy(3L));
    }
}
