package com.ject.image.api.controller;

import com.ject.image.api.exception.ImageExceptionHandler;
import com.ject.image.application.service.ImageFileValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ImageControllerTest {

    private static final int TEN_MEGABYTES = 10 * 1024 * 1024;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ImageController(new ImageFileValidator()))
                .setControllerAdvice(new ImageExceptionHandler())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    @Test
    @DisplayName("IMG-1 업로드하면 201과 보낸 순서대로 tmp key를 반환한다")
    void uploadImages_returns201WithKeys() throws Exception {
        upload("POST", file("image/jpeg", 100), file("image/png", 100))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.uploads.length()").value(2))
                .andExpect(jsonPath("$.data.uploads[0].key").value("post/tmp/3/8f14e45f.jpg"))
                .andExpect(jsonPath("$.data.uploads[1].key").value("post/tmp/3/c9f0f895.png"));
    }

    @Test
    @DisplayName("IMG-1 key 앞부분은 purpose를 따른다")
    void uploadImages_keyFollowsPurpose() throws Exception {
        upload("CHAT", file("image/webp", 100))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.uploads[0].key").value("chat/tmp/3/8f14e45f.webp"));
    }

    @Test
    @DisplayName("IMG-1 purpose가 없으면 COMMON_001")
    void uploadImages_withoutPurpose_returnsCommon001() throws Exception {
        expectCommon001(upload(null, file("image/jpeg", 100)), "purpose");
    }

    @Test
    @DisplayName("IMG-1 없는 purpose 값이면 COMMON_001")
    void uploadImages_unknownPurpose_returnsCommon001() throws Exception {
        expectCommon001(upload("FOO", file("image/jpeg", 100)), "purpose");
    }

    @Test
    @DisplayName("IMG-1 파일이 없으면 COMMON_001")
    void uploadImages_withoutFiles_returnsCommon001() throws Exception {
        expectCommon001(upload("POST"), "files");
    }

    @Test
    @DisplayName("IMG-1 파일이 6개면 COMMON_001")
    void uploadImages_sixFiles_returnsCommon001() throws Exception {
        MockMultipartFile[] files = new MockMultipartFile[6];
        for (int i = 0; i < files.length; i++) {
            files[i] = file("image/jpeg", 100);
        }
        expectCommon001(upload("POST", files), "files");
    }

    @Test
    @DisplayName("IMG-1 지원하지 않는 형식이면 몇 번째 파일인지와 형식을 담아 IMAGE_001")
    void uploadImages_unsupportedType_returnsImage001() throws Exception {
        upload("POST", file("image/jpeg", 100), file("image/gif", 100))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("IMAGE_001"))
                .andExpect(jsonPath("$.message")
                        .value("2번째 파일은 지원하지 않는 형식입니다 (image/gif). JPEG, PNG, WEBP만 올릴 수 있습니다."));
    }

    @Test
    @DisplayName("IMG-1 10MB를 넘으면 몇 번째 파일인지 담아 IMAGE_001")
    void uploadImages_tooLarge_returnsImage001() throws Exception {
        upload("POST", file("image/jpeg", TEN_MEGABYTES + 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("IMAGE_001"))
                .andExpect(jsonPath("$.message").value("1번째 파일이 10MB를 초과했습니다."));
    }

    @Test
    @DisplayName("IMG-1 정확히 10MB는 올릴 수 있다")
    void uploadImages_exactlyTenMegabytes_returns201() throws Exception {
        upload("POST", file("image/jpeg", TEN_MEGABYTES))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("IMG-2 key에 /가 있어도 이미지와 캐시 헤더를 반환한다")
    void getImage_returnsImageWithCacheHeader() throws Exception {
        mockMvc.perform(get("/api/v1/images/post/10/8f14e45f.jpg"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(header().string("Cache-Control", "private, max-age=86400"))
                .andExpect(result -> {
                    if (result.getResponse().getContentAsByteArray().length == 0) {
                        throw new AssertionError("이미지 본문이 비어 있습니다.");
                    }
                });
    }

    private ResultActions upload(String purpose, MockMultipartFile... files) throws Exception {
        MockMultipartHttpServletRequestBuilder request = multipart("/api/v1/images");
        for (MockMultipartFile file : files) {
            request.file(file);
        }
        if (purpose != null) {
            request.param("purpose", purpose);
        }
        return mockMvc.perform(request);
    }

    private static MockMultipartFile file(String contentType, int size) {
        return new MockMultipartFile("files", "photo", contentType, new byte[size]);
    }

    private static void expectCommon001(ResultActions result, String field) throws Exception {
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.errors[0].field").value(field));
    }
}
