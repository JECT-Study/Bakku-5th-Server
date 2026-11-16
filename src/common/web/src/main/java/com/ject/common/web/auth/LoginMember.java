package com.ject.common.web.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 로그인한 회원 id를 컨트롤러 파라미터로 받는다. 인증 필터가 토큰을 확인한 뒤 요청에 담아 둔 값이다.
 * <pre>
 * public ApiResponse&lt;...&gt; create(@LoginMember Long memberId, ...)
 * </pre>
 * 로그인 없이 부르는 경로(app.auth.public-paths)에서는 값이 없어 AUTH_001이 난다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginMember {

    /**
     * 인증 필터가 회원 id를 담는 요청 속성 이름
     */
    String ATTRIBUTE = LoginMember.class.getName() + ".MEMBER_ID";
}
