package com.ject.image.application.port;

import com.ject.image.application.dto.StoredImage;

import java.io.InputStream;
import java.util.Optional;

public interface ImageStorage {

    void put(String key, InputStream content, long size, String contentType);

    Optional<StoredImage> get(String key);

    boolean exists(String key);

    void copy(String fromKey, String toKey);

    void delete(String key);
}
