package com.ject.common.web.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginMemberArgumentResolverTest {

    private final LoginMemberArgumentResolver resolver = new LoginMemberArgumentResolver();

    @SuppressWarnings("unused")
    void handler(@LoginMember Long memberId, Long otherId) {
    }

    @Test
    @DisplayName("@LoginMember Long 파라미터만 처리한다")
    void supports() throws Exception {
        assertTrue(resolver.supportsParameter(parameter(0)));
        assertFalse(resolver.supportsParameter(parameter(1)));
    }

    @Test
    @DisplayName("인증 필터가 담아 둔 회원 id를 넣는다")
    void resolve() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(LoginMember.ATTRIBUTE, 7L);

        // when
        Long memberId = resolver.resolveArgument(parameter(0), null, new ServletWebRequest(request), null);

        // then
        assertEquals(7L, memberId);
    }

    @Test
    @DisplayName("회원 id가 없으면(공개 경로에서 사용) AUTH_001")
    void missing() throws Exception {
        assertThrows(LoginRequiredException.class, () -> resolver.resolveArgument(parameter(0), null,
                new ServletWebRequest(new MockHttpServletRequest()), null));
    }

    private MethodParameter parameter(int index) throws NoSuchMethodException {
        return new MethodParameter(getClass().getDeclaredMethod("handler", Long.class, Long.class), index);
    }
}
