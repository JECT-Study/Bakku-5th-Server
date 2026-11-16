package com.ject.image.api.controller;

import com.ject.image.api.dto.enums.ImagePurpose;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

final class ImageStubData {

    private static final Long USER_ID = 3L;
    private static final List<String> HASHES = List.of("8f14e45f", "c9f0f895", "45c48cce", "d3d94468", "6512bd43");
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");

    static final byte[] SAMPLE_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");

    private ImageStubData() {
    }

    static List<String> keys(ImagePurpose purpose, List<String> contentTypes) {
        String prefix = purpose.name().toLowerCase() + "/tmp/" + USER_ID + "/";
        return IntStream.range(0, contentTypes.size())
                .mapToObj(i -> prefix + HASHES.get(i) + "." + EXTENSIONS.get(contentTypes.get(i)))
                .toList();
    }
}
