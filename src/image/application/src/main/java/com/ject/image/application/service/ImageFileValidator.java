package com.ject.image.application.service;

import com.ject.image.application.dto.ImageFileInfo;
import com.ject.image.domain.exception.ImageTooLargeException;
import com.ject.image.domain.exception.UnsupportedImageTypeException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class ImageFileValidator {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_MEGABYTES = 10;
    private static final long MAX_BYTES = MAX_MEGABYTES * 1024 * 1024;

    public void validate(List<ImageFileInfo> files) {
        for (int i = 0; i < files.size(); i++) {
            int order = i + 1;
            ImageFileInfo file = files.get(i);
            if (!ALLOWED_CONTENT_TYPES.contains(file.contentType())) {
                throw new UnsupportedImageTypeException(order, file.contentType());
            }
            if (file.size() > MAX_BYTES) {
                throw new ImageTooLargeException(order, MAX_MEGABYTES);
            }
        }
    }
}
