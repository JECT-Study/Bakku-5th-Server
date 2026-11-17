package com.ject.image.application.service;

import com.ject.image.application.fake.ImageBytes;
import com.ject.image.domain.domain.ImageFormat;
import com.ject.image.domain.exception.ImageTooLargeException;
import com.ject.image.domain.exception.UnsupportedImageTypeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImageFileValidatorTest {

    private static final long TEN_MEGABYTES = 10L * 1024 * 1024;

    private final ImageFileValidator validator = new ImageFileValidator();

    @Test
    @DisplayName("파일 내용으로 형식을 판별한다")
    void validate_returnsFormat() {
        assertEquals(ImageFormat.PNG, validator.validate(1, 100, ImageBytes.PNG));
    }

    @Test
    @DisplayName("정확히 10MB는 올릴 수 있다")
    void validate_exactlyTenMegabytes() {
        assertEquals(ImageFormat.JPEG, validator.validate(1, TEN_MEGABYTES, ImageBytes.JPEG));
    }

    @Test
    @DisplayName("10MB를 넘으면 몇 번째 파일인지 담아 IMAGE_001")
    void validate_tooLarge() {
        ImageTooLargeException e = assertThrows(ImageTooLargeException.class,
                () -> validator.validate(2, TEN_MEGABYTES + 1, ImageBytes.JPEG));

        assertEquals("IMAGE_001", e.getErrorCode().getCode());
        assertEquals("2번째 파일이 10MB를 초과했습니다.", e.getMessage());
    }

    @Test
    @DisplayName("지원하지 않는 형식이면 몇 번째 파일인지 담아 IMAGE_001")
    void validate_unsupported() {
        UnsupportedImageTypeException e = assertThrows(UnsupportedImageTypeException.class,
                () -> validator.validate(3, 100, ImageBytes.GIF));

        assertEquals("IMAGE_001", e.getErrorCode().getCode());
        assertEquals("3번째 파일은 지원하지 않는 형식입니다. JPEG, PNG, WEBP만 올릴 수 있습니다.", e.getMessage());
    }
}
