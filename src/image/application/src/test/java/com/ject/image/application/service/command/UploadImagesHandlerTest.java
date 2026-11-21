package com.ject.image.application.service.command;

import com.ject.image.application.dto.ImageUpload;
import com.ject.image.application.dto.command.UploadImagesCommand;
import com.ject.image.application.fake.ImageBytes;
import com.ject.image.application.fake.InMemoryImageStorage;
import com.ject.image.application.service.ImageFileValidator;
import com.ject.image.domain.exception.ImageTooLargeException;
import com.ject.image.domain.exception.UnsupportedImageTypeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UploadImagesHandlerTest {

    private static final String UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    private final InMemoryImageStorage storage = new InMemoryImageStorage();
    private final UploadImagesHandler handler = new UploadImagesHandler(new ImageFileValidator(), storage);

    @Test
    @DisplayName("보낸 순서대로 tmp key를 만들고 판별한 형식으로 저장한다")
    void handle_storesInOrder() {
        List<String> keys = handler.handle(new UploadImagesCommand(3L, "POST",
                List.of(upload(ImageBytes.JPEG), upload(ImageBytes.PNG))));

        assertEquals(2, keys.size());
        assertTrue(keys.get(0).matches("post/tmp/3/" + UUID_PATTERN + "\\.jpg"), keys.get(0));
        assertTrue(keys.get(1).matches("post/tmp/3/" + UUID_PATTERN + "\\.png"), keys.get(1));
        assertEquals("image/jpeg", storage.contentTypeOf(keys.get(0)));
        assertEquals("image/png", storage.contentTypeOf(keys.get(1)));
    }

    @Test
    @DisplayName("key 앞부분은 용도를 따른다")
    void handle_prefixFollowsPurpose() {
        List<String> keys = handler.handle(new UploadImagesCommand(3L, "CHAT", List.of(upload(ImageBytes.WEBP))));

        assertTrue(keys.get(0).matches("chat/tmp/3/" + UUID_PATTERN + "\\.webp"), keys.get(0));
    }

    @Test
    @DisplayName("한 장이라도 지원하지 않는 형식이면 아무것도 저장하지 않는다")
    void handle_unsupported_storesNothing() {
        UploadImagesCommand command = new UploadImagesCommand(3L, "POST",
                List.of(upload(ImageBytes.JPEG), upload(ImageBytes.GIF)));

        UnsupportedImageTypeException e = assertThrows(UnsupportedImageTypeException.class,
                () -> handler.handle(command));

        assertTrue(e.getMessage().startsWith("2번째 파일"));
        assertTrue(storage.keys().isEmpty());
    }

    @Test
    @DisplayName("한 장이라도 10MB를 넘으면 아무것도 저장하지 않는다")
    void handle_tooLarge_storesNothing() {
        ImageUpload large = new ImageUpload(10L * 1024 * 1024 + 1, () -> new ByteArrayInputStream(ImageBytes.JPEG));
        UploadImagesCommand command = new UploadImagesCommand(3L, "POST", List.of(upload(ImageBytes.PNG), large));

        assertThrows(ImageTooLargeException.class, () -> handler.handle(command));
        assertTrue(storage.keys().isEmpty());
    }

    private static ImageUpload upload(byte[] content) {
        return new ImageUpload(content.length, () -> new ByteArrayInputStream(content));
    }
}
