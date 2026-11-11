package com.ject.common.web.idempotency;

import com.ject.common.core.exception.CommonErrorCode;
import com.ject.common.web.response.ApiResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdempotencyAspectTest {

    private static final String KEY = "550e8400-e29b-41d4-a716-446655440000";
    private static final String STORE_KEY = "POST:/api/v1/posts:" + KEY;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private FakeIdempotencyStore store;
    private SampleController target;
    private SampleController proxy;

    @BeforeEach
    void setUp() {
        store = new FakeIdempotencyStore();
        target = new SampleController();
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(new IdempotencyAspect(store));
        proxy = factory.getProxy();

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/posts");
        request.addHeader("Idempotency-Key", KEY);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("첫 요청이면 실행하고 성공 응답을 저장한다")
    void savesResponseOnFirstRequest() {
        // when
        ApiResponse<SampleResponse> response = proxy.create();

        // then
        assertEquals(201, response.getStatusCode().value());
        assertEquals(1, target.calls);
        assertTrue(store.findResponse(STORE_KEY).isPresent());
    }

    @Test
    @DisplayName("같은 키로 다시 오면 실행하지 않고 처음 응답을 그대로 반환한다")
    void replaysSavedResponse() {
        // given
        ApiResponse<SampleResponse> first = proxy.create();

        // when
        ApiResponse<SampleResponse> second = proxy.create();

        // then
        assertEquals(1, target.calls);
        assertEquals(first.getStatusCode(), second.getStatusCode());
        assertEquals(jsonMapper.writeValueAsString(first.getBody()), jsonMapper.writeValueAsString(second.getBody()));
    }

    @Test
    @DisplayName("같은 키가 처리 중이면 COMMON_004 예외를 던진다")
    void throwsWhenInProgress() {
        // given
        store.tryLock(STORE_KEY);

        // when
        IdempotencyException e = assertThrows(IdempotencyException.class, () -> proxy.create());

        // then
        assertEquals(CommonErrorCode.IDEMPOTENCY_IN_PROGRESS, e.getErrorCode());
        assertEquals(0, target.calls);
    }

    @Test
    @DisplayName("예외가 나면 키를 지워 같은 키로 다시 실행할 수 있다")
    void releasesKeyOnException() {
        // when
        assertThrows(IllegalStateException.class, () -> proxy.fail());

        // then
        assertFalse(store.contains(STORE_KEY));
    }

    @Test
    @DisplayName("성공이 아닌 응답은 저장하지 않는다")
    void doesNotSaveNonSuccessResponse() {
        // when
        ApiResponse<Void> response = proxy.badRequest();

        // then
        assertEquals(400, response.getStatusCode().value());
        assertFalse(store.contains(STORE_KEY));
    }

    static class SampleController {

        int calls;

        @Idempotent
        public ApiResponse<SampleResponse> create() {
            calls++;
            return ApiResponse.created(new SampleResponse(1L, "치이카와 하치와레 가챠 키링"));
        }

        @Idempotent
        public ApiResponse<Void> fail() {
            calls++;
            throw new IllegalStateException("처리 실패");
        }

        @Idempotent
        public ApiResponse<Void> badRequest() {
            calls++;
            return ApiResponse.error(CommonErrorCode.INVALID_INPUT);
        }
    }

    record SampleResponse(Long id, String title) {
    }
}
