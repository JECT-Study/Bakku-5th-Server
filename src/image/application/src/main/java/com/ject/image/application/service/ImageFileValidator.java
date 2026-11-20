package com.ject.image.application.service;

import com.ject.image.domain.domain.ImageFormat;
import com.ject.image.domain.exception.ImageTooLargeException;
import com.ject.image.domain.exception.UnsupportedImageTypeException;
import org.springframework.stereotype.Component;

@Component
public class ImageFileValidator {

    private static final long MAX_MEGABYTES = 10;
    private static final long MAX_BYTES = MAX_MEGABYTES * 1024 * 1024;

    public ImageFormat validate(int order, long size, byte[] header) {
        if (size > MAX_BYTES) {
            throw new ImageTooLargeException(order, MAX_MEGABYTES);
        }
        return ImageFormat.detect(header)
                .orElseThrow(() -> new UnsupportedImageTypeException(order));
    }
}
