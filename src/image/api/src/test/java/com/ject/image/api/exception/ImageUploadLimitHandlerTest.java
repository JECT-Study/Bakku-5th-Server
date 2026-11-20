package com.ject.image.api.exception;

import com.ject.common.web.response.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImageUploadLimitHandlerTest {

    @Test
    @DisplayName("요청 전체가 최대 용량을 넘으면 최대 용량을 담아 IMAGE_001")
    void handleMaxUploadSize_returnsImage001() {
        ImageUploadLimitHandler handler = new ImageUploadLimitHandler(DataSize.ofMegabytes(50));

        ApiResponse<Void> response = handler.handleMaxUploadSize(
                new MaxUploadSizeExceededException(DataSize.ofMegabytes(50).toBytes()));

        assertEquals(400, response.getStatusCode().value());
        assertEquals("IMAGE_001", response.getBody().code());
        assertEquals("한 번에 올릴 수 있는 용량(50MB)을 초과했습니다.", response.getBody().message());
    }
}
