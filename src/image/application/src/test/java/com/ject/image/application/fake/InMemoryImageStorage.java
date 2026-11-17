package com.ject.image.application.fake;

import com.ject.image.application.dto.StoredImage;
import com.ject.image.application.port.ImageStorage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class InMemoryImageStorage implements ImageStorage {

    private final Map<String, StoredObject> objects = new LinkedHashMap<>();

    @Override
    public void put(String key, InputStream content, long size, String contentType) {
        try {
            objects.put(key, new StoredObject(content.readAllBytes(), contentType));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Optional<StoredImage> get(String key) {
        return Optional.ofNullable(objects.get(key))
                .map(object -> new StoredImage(
                        new ByteArrayInputStream(object.content()), object.content().length, object.contentType()));
    }

    @Override
    public boolean exists(String key) {
        return objects.containsKey(key);
    }

    @Override
    public void copy(String fromKey, String toKey) {
        StoredObject object = objects.get(fromKey);
        if (object == null) {
            throw new IllegalStateException("없는 객체를 복사했습니다. key=" + fromKey);
        }
        objects.put(toKey, object);
    }

    @Override
    public void delete(String key) {
        objects.remove(key);
    }

    public void put(String key, byte[] content, String contentType) {
        objects.put(key, new StoredObject(content, contentType));
    }

    public Set<String> keys() {
        return objects.keySet();
    }

    public String contentTypeOf(String key) {
        return objects.get(key).contentType();
    }

    private record StoredObject(byte[] content, String contentType) {
    }
}
