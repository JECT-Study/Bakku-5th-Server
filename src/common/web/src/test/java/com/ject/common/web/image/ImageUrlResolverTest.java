package com.ject.common.web.image;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImageUrlResolverTest {

    @Test
    @DisplayName("서버 주소 뒤에 이미지 조회 경로와 key를 붙인다")
    void resolve() {
        ImageUrlResolver resolver = new ImageUrlResolver("https://dev-api.bakku.com");

        assertEquals("https://dev-api.bakku.com/api/v1/images/post/10/8f14e45f.jpg",
                resolver.resolve("post/10/8f14e45f.jpg"));
    }

    @Test
    @DisplayName("서버 주소 끝에 /가 있어도 한 번만 붙인다")
    void resolve_trailingSlash() {
        ImageUrlResolver resolver = new ImageUrlResolver("http://localhost:8080/");

        assertEquals("http://localhost:8080/api/v1/images/post/10/8f14e45f.jpg",
                resolver.resolve("post/10/8f14e45f.jpg"));
    }
}
