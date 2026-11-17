package com.ject.image.domain.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImageFormatTest {

    @Test
    @DisplayName("JPEG 시그니처를 JPEG로 판별한다")
    void detect_jpeg() {
        byte[] header = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0};

        assertEquals(Optional.of(ImageFormat.JPEG), ImageFormat.detect(header));
    }

    @Test
    @DisplayName("PNG 시그니처를 PNG로 판별한다")
    void detect_png() {
        byte[] header = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 0};

        assertEquals(Optional.of(ImageFormat.PNG), ImageFormat.detect(header));
    }

    @Test
    @DisplayName("RIFF....WEBP를 WEBP로 판별한다")
    void detect_webp() {
        byte[] header = "RIFF\0\0\0\0WEBP".getBytes(StandardCharsets.US_ASCII);

        assertEquals(Optional.of(ImageFormat.WEBP), ImageFormat.detect(header));
    }

    @Test
    @DisplayName("WEBP 표시가 없는 RIFF 파일은 판별하지 않는다")
    void detect_riffWithoutWebp() {
        byte[] header = "RIFF\0\0\0\0WAVE".getBytes(StandardCharsets.US_ASCII);

        assertEquals(Optional.empty(), ImageFormat.detect(header));
    }

    @Test
    @DisplayName("GIF는 판별하지 않는다")
    void detect_gif() {
        byte[] header = "GIF89a\0\0\0\0\0\0".getBytes(StandardCharsets.US_ASCII);

        assertEquals(Optional.empty(), ImageFormat.detect(header));
    }

    @Test
    @DisplayName("시그니처보다 짧은 파일은 판별하지 않는다")
    void detect_tooShort() {
        assertEquals(Optional.empty(), ImageFormat.detect(new byte[]{(byte) 0xFF, (byte) 0xD8}));
        assertEquals(Optional.empty(), ImageFormat.detect(new byte[0]));
    }

    @Test
    @DisplayName("확장자로 형식을 찾는다")
    void fromExtension() {
        assertEquals(Optional.of(ImageFormat.JPEG), ImageFormat.fromExtension("jpg"));
        assertEquals(Optional.empty(), ImageFormat.fromExtension("gif"));
    }
}
