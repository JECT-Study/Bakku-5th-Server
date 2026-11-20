package com.ject.image.api.controller;

import com.jayway.jsonpath.JsonPath;
import com.ject.common.core.cqrs.command.Command;
import com.ject.common.core.cqrs.command.CommandBus;
import com.ject.common.core.cqrs.query.Query;
import com.ject.common.core.cqrs.query.QueryBus;
import com.ject.common.web.auth.LoginMember;
import com.ject.common.web.auth.LoginMemberArgumentResolver;
import com.ject.image.api.exception.ImageExceptionHandler;
import com.ject.image.application.dto.command.UploadImagesCommand;
import com.ject.image.application.dto.query.GetImageQuery;
import com.ject.image.application.service.ImageFileValidator;
import com.ject.image.application.service.command.UploadImagesHandler;
import com.ject.image.application.service.query.GetImageHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ImageControllerTest {

    private static final int TEN_MEGABYTES = 10 * 1024 * 1024;
    private static final long MEMBER_ID = 3L;
    private static final String UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
    private static final String FILE = "8f14e45f-ceea-467f-a8f5-6e8e6e5c7d1a.jpg";

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 0, 3, 4};
    private static final byte[] GIF = "GIF89a\0\0\0\0\0\0".getBytes(StandardCharsets.US_ASCII);

    private InMemoryImageStorage storage;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        storage = new InMemoryImageStorage();
        mockMvc = mockMvcWithCacheMaxAge(Duration.ofDays(1));
    }

    private MockMvc mockMvcWithCacheMaxAge(Duration cacheMaxAge) {
        UploadImagesHandler uploadHandler = new UploadImagesHandler(new ImageFileValidator(), storage);
        GetImageHandler getHandler = new GetImageHandler(storage);
        CommandBus commandBus = new CommandBus() {
            @Override
            @SuppressWarnings("unchecked")
            public <R> R send(Command<R> command) {
                return (R) uploadHandler.handle((UploadImagesCommand) command);
            }
        };
        QueryBus queryBus = new QueryBus() {
            @Override
            @SuppressWarnings("unchecked")
            public <R> R send(Query<R> query) {
                return (R) getHandler.handle((GetImageQuery) query);
            }
        };
        return MockMvcBuilders.standaloneSetup(new ImageController(commandBus, queryBus, cacheMaxAge))
                .setCustomArgumentResolvers(new LoginMemberArgumentResolver())
                .setControllerAdvice(new ImageExceptionHandler())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    @Test
    @DisplayName("IMG-1 업로드하면 201과 보낸 순서대로 tmp key를 반환한다")
    void uploadImages_returns201WithKeys() throws Exception {
        upload("POST", file(JPEG), file(PNG))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.uploads.length()").value(2))
                .andExpect(jsonPath("$.data.uploads[0].key").value(matchesPattern("post/tmp/3/" + UUID_PATTERN + "\\.jpg")))
                .andExpect(jsonPath("$.data.uploads[1].key").value(matchesPattern("post/tmp/3/" + UUID_PATTERN + "\\.png")));
    }

    @Test
    @DisplayName("IMG-1 key 앞부분은 purpose를 따른다")
    void uploadImages_keyFollowsPurpose() throws Exception {
        upload("CHAT", file(JPEG))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.uploads[0].key").value(matchesPattern("chat/tmp/3/" + UUID_PATTERN + "\\.jpg")));
    }

    @Test
    @DisplayName("IMG-1 purpose가 없으면 COMMON_001")
    void uploadImages_withoutPurpose_returnsCommon001() throws Exception {
        expectCommon001(upload(null, file(JPEG)), "purpose");
    }

    @Test
    @DisplayName("IMG-1 없는 purpose 값이면 COMMON_001")
    void uploadImages_unknownPurpose_returnsCommon001() throws Exception {
        expectCommon001(upload("FOO", file(JPEG)), "purpose");
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
            files[i] = file(JPEG);
        }
        expectCommon001(upload("POST", files), "files");
    }

    @Test
    @DisplayName("IMG-1 파일 내용이 지원하지 않는 형식이면 몇 번째 파일인지 담아 IMAGE_001")
    void uploadImages_unsupportedType_returnsImage001() throws Exception {
        upload("POST", file(JPEG), file(GIF))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("IMAGE_001"))
                .andExpect(jsonPath("$.message")
                        .value("2번째 파일은 지원하지 않는 형식입니다. JPEG, PNG, WEBP만 올릴 수 있습니다."));
    }

    @Test
    @DisplayName("IMG-1 10MB를 넘으면 몇 번째 파일인지 담아 IMAGE_001")
    void uploadImages_tooLarge_returnsImage001() throws Exception {
        upload("POST", file(jpegOfSize(TEN_MEGABYTES + 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("IMAGE_001"))
                .andExpect(jsonPath("$.message").value("1번째 파일이 10MB를 초과했습니다."));
    }

    @Test
    @DisplayName("IMG-1 정확히 10MB는 올릴 수 있다")
    void uploadImages_exactlyTenMegabytes_returns201() throws Exception {
        upload("POST", file(jpegOfSize(TEN_MEGABYTES)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("IMG-2 올린 이미지를 올린 회원이 조회하면 원본과 캐시 헤더를 반환한다")
    void getImage_uploadedTmp_returnsImage() throws Exception {
        MvcResult uploaded = upload("POST", file(JPEG)).andReturn();
        String key = JsonPath.read(
                uploaded.getResponse().getContentAsString(), "$.data.uploads[0].key");

        mockMvc.perform(get("/api/v1/images/" + key).requestAttr(LoginMember.ATTRIBUTE, MEMBER_ID))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(header().string("Content-Length", String.valueOf(JPEG.length)))
                .andExpect(header().string("Cache-Control", "max-age=86400, private"))
                .andExpect(content().bytes(JPEG));
    }

    @Test
    @DisplayName("IMG-2 교환글 정식 이미지는 다른 회원도 조회할 수 있다")
    void getImage_postFinal_returnsImage() throws Exception {
        storage.put("post/10/" + FILE, JPEG, "image/jpeg");

        getImage("post/10/" + FILE, 4L)
                .andExpect(status().isOk())
                .andExpect(content().bytes(JPEG));
    }

    @Test
    @DisplayName("IMG-2 다른 회원의 tmp 이미지는 IMAGE_003")
    void getImage_otherMembersTmp_returnsImage003() throws Exception {
        storage.put("post/tmp/3/" + FILE, JPEG, "image/jpeg");

        getImage("post/tmp/3/" + FILE, 4L)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("IMAGE_003"));
    }

    @Test
    @DisplayName("IMG-2 없는 이미지는 IMAGE_002")
    void getImage_notStored_returnsImage002() throws Exception {
        getImage("post/10/" + FILE, MEMBER_ID)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("IMAGE_002"));
    }

    @Test
    @DisplayName("IMG-2 설정한 저장 시간이 max-age에 반영된다")
    void getImage_usesConfiguredCacheMaxAge() throws Exception {
        storage.put("post/10/" + FILE, JPEG, "image/jpeg");

        mockMvcWithCacheMaxAge(Duration.ofHours(1))
                .perform(get("/api/v1/images/post/10/" + FILE).requestAttr(LoginMember.ATTRIBUTE, MEMBER_ID))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "max-age=3600, private"));
    }

    private ResultActions upload(String purpose, MockMultipartFile... files) throws Exception {
        MockMultipartHttpServletRequestBuilder request = multipart("/api/v1/images");
        for (MockMultipartFile file : files) {
            request.file(file);
        }
        if (purpose != null) {
            request.param("purpose", purpose);
        }
        request.requestAttr(LoginMember.ATTRIBUTE, MEMBER_ID);
        return mockMvc.perform(request);
    }

    private ResultActions getImage(String key, Long memberId) throws Exception {
        return mockMvc.perform(get("/api/v1/images/" + key).requestAttr(LoginMember.ATTRIBUTE, memberId));
    }

    private static MockMultipartFile file(byte[] content) {
        return new MockMultipartFile("files", "photo", "application/octet-stream", content);
    }

    private static byte[] jpegOfSize(int size) {
        byte[] content = new byte[size];
        System.arraycopy(JPEG, 0, content, 0, JPEG.length);
        return content;
    }

    private static void expectCommon001(ResultActions result, String field) throws Exception {
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.errors[0].field").value(field));
    }
}
