package com.ject.post.api.controller;

import com.ject.post.api.exception.PostExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PostControllerTest {

    private static final String CREATE_BODY = """
            {
              "imageKeys": ["post/tmp/3/8f14e45f.jpg", "post/tmp/3/c9f0f895.jpg"],
              "title": "치이카와 하치와레 가챠 키링",
              "category": "CHARACTER",
              "opened": false,
              "tags": ["치이카와", "하치와레"],
              "description": "치이카와 가챠 3탄에서 뽑았어요.",
              "wantedItem": "치이카와 가챠 키링",
              "place": {
                "lat": 37.5563,
                "lng": 126.9236,
                "name": "스타벅스 홍대동교점",
                "regionName": "서울특별시 마포구 동교동",
                "memo": "AK플라자 주변"
              },
              "nowExchangeMinutes": 60
            }
            """;

    private static final String UPDATE_BODY = """
            {
              "imageKeys": ["post/10/8f14e45f.jpg", "post/tmp/3/c9f0f895.jpg"],
              "title": "치이카와 하치와레 가챠 키링",
              "category": "CHARACTER",
              "opened": true,
              "tags": ["치이카와"],
              "description": "한 번 개봉했어요.",
              "wantedItem": null,
              "place": {
                "lat": 37.5563,
                "lng": 126.9236,
                "name": "스타벅스 홍대동교점",
                "regionName": "서울특별시 마포구 동교동",
                "memo": null
              }
            }
            """;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PostController())
                .setControllerAdvice(new PostExceptionHandler())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    @Test
    @DisplayName("PO-1 작성하면 201과 작성한 글을 반환하고, canRequest는 응답에 없다")
    void createPost_returns201WithPost() throws Exception {
        createPost(CREATE_BODY)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.data.isMine").value(true))
                .andExpect(jsonPath("$.data.place.regionName").value("마포구 동교동"))
                .andExpect(jsonPath("$.data.nowExchangeEndsAt").value("2026-11-02T15:30:00+09:00"))
                .andExpect(jsonPath("$.data.author.completedCount").value(12))
                .andExpect(jsonPath("$.data", hasEntry(equalTo("chatRoomId"), nullValue())))
                .andExpect(jsonPath("$.data.canRequest").doesNotExist());
    }

    @Test
    @DisplayName("PO-1 업로드 tmp key는 글 경로 key로 바뀐다")
    void createPost_movesTmpImageKey() throws Exception {
        createPost(CREATE_BODY)
                .andExpect(jsonPath("$.data.images[0].key").value("post/10/8f14e45f.jpg"))
                .andExpect(jsonPath("$.data.images[0].url").value("https://{cdn}/post/10/8f14e45f.jpg"));
    }

    @Test
    @DisplayName("PO-1 wantedItem을 생략하면 null(아무거나)로 응답하고 wantedAny 필드는 없다")
    void createPost_withoutWantedItem_returnsNullWantedItem() throws Exception {
        createPost(CREATE_BODY.replace("\"wantedItem\": \"치이카와 가챠 키링\",", ""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data", hasEntry(equalTo("wantedItem"), nullValue())))
                .andExpect(jsonPath("$.data.wantedAny").doesNotExist());
    }

    @Test
    @DisplayName("PO-1 Idempotency-Key 헤더가 없으면 COMMON_001")
    void createPost_withoutIdempotencyKey_returnsCommon001() throws Exception {
        mockMvc.perform(post("/api/v1/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_001"))
                .andExpect(jsonPath("$.errors[0].field").value("Idempotency-Key"));
    }

    @Test
    @DisplayName("PO-1 wantedItem이 공백뿐이면 COMMON_001")
    void createPost_blankWantedItem_returnsCommon001() throws Exception {
        expectCommon001(createPost(CREATE_BODY.replace("\"치이카와 가챠 키링\"", "\"  \"")));
    }

    @Test
    @DisplayName("PO-1 imageKeys가 중복되면 COMMON_001")
    void createPost_duplicateImageKeys_returnsCommon001() throws Exception {
        expectCommon001(createPost(CREATE_BODY.replace(
                "[\"post/tmp/3/8f14e45f.jpg\", \"post/tmp/3/c9f0f895.jpg\"]",
                "[\"post/tmp/3/8f14e45f.jpg\", \"post/tmp/3/8f14e45f.jpg\"]")));
    }

    @Test
    @DisplayName("PO-1 태그에 #이 있으면 COMMON_001")
    void createPost_tagWithHash_returnsCommon001() throws Exception {
        expectCommon001(createPost(CREATE_BODY.replace("[\"치이카와\", \"하치와레\"]", "[\"#치이카와\"]")));
    }

    @Test
    @DisplayName("PO-1 지금 교환 시간이 15·30·60·120·180분이 아니면 COMMON_001")
    void createPost_nowExchange45_returnsCommon001() throws Exception {
        expectCommon001(createPost(CREATE_BODY.replace("\"nowExchangeMinutes\": 60", "\"nowExchangeMinutes\": 45")));
    }

    @Test
    @DisplayName("PO-3 상세는 canRequest를 포함한다")
    void getPost_includesCanRequest() throws Exception {
        mockMvc.perform(get("/api/v1/posts/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.isMine").value(false))
                .andExpect(jsonPath("$.data.canRequest").value(true));
    }

    @Test
    @DisplayName("PO-3 postId 형식이 틀리면 COMMON_001")
    void getPost_invalidId_returnsCommon001() throws Exception {
        expectCommon001(mockMvc.perform(get("/api/v1/posts/abc")));
    }

    @Test
    @DisplayName("PO-4 수정 응답에는 canRequest가 없고, 보내지 않은 memo는 null이다")
    void updatePost_omitsCanRequest() throws Exception {
        mockMvc.perform(put("/api/v1/posts/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.images[0].key").value("post/10/8f14e45f.jpg"))
                .andExpect(jsonPath("$.data.images[1].key").value("post/10/c9f0f895.jpg"))
                .andExpect(jsonPath("$.data.place", hasEntry(equalTo("memo"), nullValue())))
                .andExpect(jsonPath("$.data.canRequest").doesNotExist());
    }

    @Test
    @DisplayName("PO-5 삭제하면 data는 null이다")
    void deletePost_returnsNullData() throws Exception {
        mockMvc.perform(delete("/api/v1/posts/10"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"success\":true,\"data\":null}", JsonCompareMode.STRICT));
    }

    @Test
    @DisplayName("PO-5 postId 형식이 틀리면 COMMON_001")
    void deletePost_invalidId_returnsCommon001() throws Exception {
        expectCommon001(mockMvc.perform(delete("/api/v1/posts/abc")));
    }

    @Test
    @DisplayName("PO-10 연관 채팅 여부를 반환한다")
    void getRelatedChats_returnsFlag() throws Exception {
        mockMvc.perform(get("/api/v1/posts/10/related-chats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasRelatedChat").value(true));
    }

    @Test
    @DisplayName("PO-6 지금 교환을 끄면 data는 null이다")
    void turnOffNowExchange_returnsNullData() throws Exception {
        mockMvc.perform(post("/api/v1/posts/10/now-exchange/off"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"success\":true,\"data\":null}", JsonCompareMode.STRICT));
    }

    private ResultActions createPost(String body) throws Exception {
        return mockMvc.perform(post("/api/v1/posts")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static void expectCommon001(ResultActions result) throws Exception {
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_001"));
    }
}
