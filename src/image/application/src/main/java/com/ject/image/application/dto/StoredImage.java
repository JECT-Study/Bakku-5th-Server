package com.ject.image.application.dto;

import java.io.InputStream;

public record StoredImage(InputStream content, long size, String contentType) {
}
