package com.ject.image.application.service;

import com.ject.image.application.exception.ImageKeyRejectedException;
import com.ject.image.application.fake.ImageBytes;
import com.ject.image.application.fake.InMemoryImageStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImageAttachmentServiceTest {

    private static final String A = "8f14e45f-ceea-467f-a8f5-6e8e6e5c7d1a.jpg";
    private static final String B = "c9f0f895-fb98-4b91-99f5-1e4b5c3d2a10.png";

    private final InMemoryImageStorage storage = new InMemoryImageStorage();
    private final ImageAttachmentService service = new ImageAttachmentService(storage);

    @Test
    @DisplayName("tmp 이미지를 대상 경로로 복사하고 정식 key를 같은 순서로 돌려준다. tmp는 남긴다")
    void finalizeImages_copiesTmp() {
        storage.put("post/tmp/3/" + A, ImageBytes.JPEG, "image/jpeg");
        storage.put("post/tmp/3/" + B, ImageBytes.PNG, "image/png");

        List<String> keys = service.finalizeImages("POST", 3L, 10L, List.of("post/tmp/3/" + B, "post/tmp/3/" + A));

        assertEquals(List.of("post/10/" + B, "post/10/" + A), keys);
        assertEquals(Set.of("post/tmp/3/" + A, "post/tmp/3/" + B, "post/10/" + A, "post/10/" + B), storage.keys());
        assertEquals("image/png", storage.contentTypeOf("post/10/" + B));
    }

    @Test
    @DisplayName("그 대상의 정식 key는 그대로 돌려준다")
    void finalizeImages_keepsFinal() {
        storage.put("post/10/" + A, ImageBytes.JPEG, "image/jpeg");
        storage.put("post/tmp/3/" + B, ImageBytes.PNG, "image/png");

        List<String> keys = service.finalizeImages("POST", 3L, 10L, List.of("post/10/" + A, "post/tmp/3/" + B));

        assertEquals(List.of("post/10/" + A, "post/10/" + B), keys);
    }

    @Test
    @DisplayName("다른 회원이 올린 tmp key면 몇 번째인지 담아 거절하고 아무것도 복사하지 않는다")
    void finalizeImages_otherUploader() {
        storage.put("post/tmp/3/" + A, ImageBytes.JPEG, "image/jpeg");
        storage.put("post/tmp/4/" + B, ImageBytes.PNG, "image/png");

        ImageKeyRejectedException e = assertThrows(ImageKeyRejectedException.class,
                () -> service.finalizeImages("POST", 3L, 10L, List.of("post/tmp/3/" + A, "post/tmp/4/" + B)));

        assertEquals(2, e.getOrder());
        assertEquals("post/tmp/4/" + B, e.getKey());
        assertEquals(Set.of("post/tmp/3/" + A, "post/tmp/4/" + B), storage.keys());
    }

    @Test
    @DisplayName("저장소에 없는 tmp key는 거절한다")
    void finalizeImages_notStored() {
        assertThrows(ImageKeyRejectedException.class,
                () -> service.finalizeImages("POST", 3L, 10L, List.of("post/tmp/3/" + A)));
    }

    @Test
    @DisplayName("다른 대상의 정식 key는 거절한다")
    void finalizeImages_otherTarget() {
        storage.put("post/11/" + A, ImageBytes.JPEG, "image/jpeg");

        assertThrows(ImageKeyRejectedException.class,
                () -> service.finalizeImages("POST", 3L, 10L, List.of("post/11/" + A)));
    }

    @Test
    @DisplayName("용도가 다른 key는 거절한다")
    void finalizeImages_otherPurpose() {
        storage.put("chat/tmp/3/" + A, ImageBytes.JPEG, "image/jpeg");

        assertThrows(ImageKeyRejectedException.class,
                () -> service.finalizeImages("POST", 3L, 10L, List.of("chat/tmp/3/" + A)));
    }

    @Test
    @DisplayName("형식이 틀린 key는 거절한다")
    void finalizeImages_invalidKey() {
        ImageKeyRejectedException e = assertThrows(ImageKeyRejectedException.class,
                () -> service.finalizeImages("POST", 3L, 10L, List.of("post/tmp/3/8f14e45f.jpg")));

        assertEquals(1, e.getOrder());
    }

    @Test
    @DisplayName("빠진 이미지는 그 대상의 정식 key만 지우고 나머지는 건너뛴다")
    void discardImages_deletesOnlyOwnFinal() {
        storage.put("post/10/" + A, ImageBytes.JPEG, "image/jpeg");
        storage.put("post/11/" + A, ImageBytes.JPEG, "image/jpeg");
        storage.put("post/tmp/3/" + A, ImageBytes.JPEG, "image/jpeg");
        storage.put("chat/10/" + A, ImageBytes.JPEG, "image/jpeg");

        service.discardImages("POST", 10L, List.of(
                "post/10/" + A, "post/11/" + A, "post/tmp/3/" + A, "chat/10/" + A, "post/10/../x.jpg"));

        assertEquals(Set.of("post/11/" + A, "post/tmp/3/" + A, "chat/10/" + A), storage.keys());
    }
}
