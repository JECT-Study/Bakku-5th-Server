package com.ject.common.web.idempotency;

import com.ject.common.web.response.ApiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record CachedResponse(
        int status,
        Map<String, List<String>> headers,
        ApiResponse.ResponseBody<?> body
) {
    public static CachedResponse from(ApiResponse<?> response) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        response.getHeaders().forEach(headers::put);
        return new CachedResponse(response.getStatusCode().value(), headers, response.getBody());
    }

    public ApiResponse<?> toApiResponse() {
        HttpHeaders httpHeaders = new HttpHeaders();
        headers.forEach(httpHeaders::put);
        return new ApiResponse<>(body, HttpStatusCode.valueOf(status), httpHeaders);
    }
}
