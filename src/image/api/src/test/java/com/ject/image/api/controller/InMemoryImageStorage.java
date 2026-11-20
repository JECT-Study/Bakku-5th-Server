package com.ject.image.api.controller;

import com.ject.image.application.dto.StoredImage;
import com.ject.image.application.port.ImageStorage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class InMemoryImageStorage implements ImageStorage {

    private final Map<String, byte[]> contents = new HashMap<>();
    private final Map<String, String> contentTypes = new HashMap<>();

    @Override
    public void put(String key, InputStream content, long size, String contentType) {
        try {
            put(key, content.readAllBytes(), contentType);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void put(String key, byte[] content, String contentType) {
        contents.put(key, content);
        contentTypes.put(key, contentType);
    }

    @Override
    public Optional<StoredImage> get(String key) {
        return Optional.ofNullable(contents.get(key))
                .map(content -> new StoredImage(new ByteArrayInputStream(content), content.length, contentTypes.get(key)));
    }

    @Override
    public boolean exists(String key) {
        return contents.containsKey(key);
    }

    @Override
    public void copy(String fromKey, String toKey) {
        put(toKey, contents.get(fromKey), contentTypes.get(fromKey));
    }

    @Override
    public void delete(String key) {
        contents.remove(key);
        contentTypes.remove(key);
    }
}
