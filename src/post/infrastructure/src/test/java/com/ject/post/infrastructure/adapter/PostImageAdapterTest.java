package com.ject.post.infrastructure.adapter;

import com.ject.image.application.contract.ImageAttachment;
import com.ject.image.application.exception.ImageKeyRejectedException;
import com.ject.post.domain.exception.InvalidImageKeyException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PostImageAdapterTest {

    @Test
    @DisplayName("게시글 용도 · 작성자 · 글 ID로 이미지 모듈에 확정을 요청하고 정식 key를 돌려준다")
    void attachToPost_returnsFinalKeys() {
        // given
        RecordingImageAttachment attachment = new RecordingImageAttachment(null);
        PostImageAdapter adapter = new PostImageAdapter(attachment);

        // when
        List<String> keys = adapter.attachToPost(3L, 10L, List.of("post/tmp/3/a.jpg"));

        // then
        assertEquals(List.of("post/10/a.jpg"), keys);
        assertEquals("POST", attachment.purpose);
        assertEquals(3L, attachment.uploaderId);
        assertEquals(10L, attachment.targetId);
    }

    @Test
    @DisplayName("이미지 모듈이 거절하면 몇 번째인지 담아 POST_005로 바꾼다")
    void attachToPost_rejected_throwsPost005() {
        // given
        PostImageAdapter adapter = new PostImageAdapter(
                new RecordingImageAttachment(new ImageKeyRejectedException(2, "post/tmp/7/b.jpg")));

        // when
        InvalidImageKeyException e = assertThrows(InvalidImageKeyException.class,
                () -> adapter.attachToPost(3L, 10L, List.of("post/tmp/3/a.jpg", "post/tmp/7/b.jpg")));

        // then
        assertEquals("POST_005", e.getErrorCode().getCode());
        assertEquals("2번째 이미지를 사용할 수 없습니다. key=post/tmp/7/b.jpg", e.getMessage());
    }

    private static class RecordingImageAttachment implements ImageAttachment {

        private final ImageKeyRejectedException rejection;
        private String purpose;
        private Long uploaderId;
        private Long targetId;

        RecordingImageAttachment(ImageKeyRejectedException rejection) {
            this.rejection = rejection;
        }

        @Override
        public List<String> finalizeImages(String purpose, Long uploaderId, Long targetId, List<String> keys) {
            if (rejection != null) {
                throw rejection;
            }
            this.purpose = purpose;
            this.uploaderId = uploaderId;
            this.targetId = targetId;
            return keys.stream()
                    .map(key -> key.replace("post/tmp/" + uploaderId + "/", "post/" + targetId + "/"))
                    .toList();
        }

        @Override
        public void discardImages(String purpose, Long targetId, List<String> keys) {
            throw new UnsupportedOperationException();
        }
    }
}
