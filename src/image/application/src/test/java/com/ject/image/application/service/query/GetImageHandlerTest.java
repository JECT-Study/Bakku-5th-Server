package com.ject.image.application.service.query;

import com.ject.image.application.dto.StoredImage;
import com.ject.image.application.dto.query.GetImageQuery;
import com.ject.image.application.fake.ImageBytes;
import com.ject.image.application.fake.InMemoryImageStorage;
import com.ject.image.domain.exception.ImageAccessDeniedException;
import com.ject.image.domain.exception.ImageNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetImageHandlerTest {

    private static final String FILE = "8f14e45f-ceea-467f-a8f5-6e8e6e5c7d1a.jpg";

    private final InMemoryImageStorage storage = new InMemoryImageStorage();
    private final GetImageHandler handler = new GetImageHandler(storage);

    @Test
    @DisplayName("올린 회원은 자기 tmp 이미지를 볼 수 있다")
    void handle_tmpByUploader() throws IOException {
        storage.put("post/tmp/3/" + FILE, ImageBytes.JPEG, "image/jpeg");

        StoredImage image = handler.handle(new GetImageQuery("post/tmp/3/" + FILE, 3L));

        assertEquals("image/jpeg", image.contentType());
        assertEquals(ImageBytes.JPEG.length, image.size());
        assertArrayEquals(ImageBytes.JPEG, image.content().readAllBytes());
    }

    @Test
    @DisplayName("다른 회원의 tmp 이미지는 IMAGE_003")
    void handle_tmpByOther() {
        storage.put("post/tmp/3/" + FILE, ImageBytes.JPEG, "image/jpeg");

        assertThrows(ImageAccessDeniedException.class,
                () -> handler.handle(new GetImageQuery("post/tmp/3/" + FILE, 4L)));
    }

    @Test
    @DisplayName("교환글 정식 이미지는 누구나 볼 수 있다")
    void handle_postFinal() {
        storage.put("post/10/" + FILE, ImageBytes.JPEG, "image/jpeg");

        assertEquals("image/jpeg", handler.handle(new GetImageQuery("post/10/" + FILE, 4L)).contentType());
    }

    @Test
    @DisplayName("교환 정식 이미지는 IMAGE_003")
    void handle_exchangeFinal() {
        storage.put("exchange/10/" + FILE, ImageBytes.JPEG, "image/jpeg");

        assertThrows(ImageAccessDeniedException.class,
                () -> handler.handle(new GetImageQuery("exchange/10/" + FILE, 3L)));
    }

    @Test
    @DisplayName("저장소에 없으면 IMAGE_002")
    void handle_notStored() {
        assertThrows(ImageNotFoundException.class,
                () -> handler.handle(new GetImageQuery("post/10/" + FILE, 3L)));
    }

    @Test
    @DisplayName("형식이 틀린 key는 저장소를 보지 않고 IMAGE_002")
    void handle_invalidKey() {
        assertThrows(ImageNotFoundException.class,
                () -> handler.handle(new GetImageQuery("post/10/../secret.jpg", 3L)));
    }
}
