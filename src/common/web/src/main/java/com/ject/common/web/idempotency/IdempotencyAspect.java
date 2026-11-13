package com.ject.common.web.idempotency;

import com.ject.common.web.auth.LoginMember;
import com.ject.common.web.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

@Aspect
@Component
@RequiredArgsConstructor
public class IdempotencyAspect {

    private static final String HEADER = "Idempotency-Key";
    private static final String ANONYMOUS = "anonymous";

    private final IdempotencyStore store;

    @Around("@annotation(idempotent)")
    public Object handle(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        HttpServletRequest request = currentRequest();
        String idempotencyKey = request.getHeader(HEADER);
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return joinPoint.proceed();
        }
        Object memberId = request.getAttribute(LoginMember.ATTRIBUTE);
        String owner = memberId == null ? ANONYMOUS : memberId.toString();
        String key = owner + ":" + request.getMethod() + ":" + request.getRequestURI() + ":" + idempotencyKey;

        if (!store.tryLock(key)) {
            return store.findResponse(key)
                    .map(CachedResponse::toApiResponse)
                    .orElseThrow(IdempotencyException::new);
        }

        try {
            Object result = joinPoint.proceed();
            if (result instanceof ApiResponse<?> response && response.getStatusCode().is2xxSuccessful()) {
                store.saveResponse(key, CachedResponse.from(response), Duration.ofHours(idempotent.ttlHours()));
            } else {
                store.release(key);
            }
            return result;
        } catch (Throwable e) {
            store.release(key);
            throw e;
        }
    }

    private HttpServletRequest currentRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
    }

}
