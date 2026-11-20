package com.ject.common.web.image;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ImageUrlResolver {

    private static final String IMAGE_PATH = "/api/v1/images/";

    private final String baseUrl;

    public ImageUrlResolver(@Value("${app.image.base-url}") String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public String resolve(String key) {
        return baseUrl + IMAGE_PATH + key;
    }
}
