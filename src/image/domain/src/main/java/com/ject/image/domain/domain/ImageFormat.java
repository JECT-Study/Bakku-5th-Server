package com.ject.image.domain.domain;

import lombok.Getter;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

@Getter
public enum ImageFormat {

    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    WEBP("image/webp", "webp");

    public static final int HEADER_SIZE = 12;

    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final byte[] RIFF = "RIFF".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] WEBP_MARK = "WEBP".getBytes(StandardCharsets.US_ASCII);

    private final String contentType;
    private final String extension;

    ImageFormat(String contentType, String extension) {
        this.contentType = contentType;
        this.extension = extension;
    }

    public static Optional<ImageFormat> detect(byte[] header) {
        if (matches(header, 0, JPEG_SIGNATURE)) {
            return Optional.of(JPEG);
        }
        if (matches(header, 0, PNG_SIGNATURE)) {
            return Optional.of(PNG);
        }
        if (matches(header, 0, RIFF) && matches(header, 8, WEBP_MARK)) {
            return Optional.of(WEBP);
        }
        return Optional.empty();
    }

    public static Optional<ImageFormat> fromExtension(String extension) {
        return Arrays.stream(values())
                .filter(format -> format.extension.equals(extension))
                .findFirst();
    }

    private static boolean matches(byte[] header, int offset, byte[] expected) {
        if (header.length < offset + expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if (header[offset + i] != expected[i]) {
                return false;
            }
        }
        return true;
    }
}
