package com.ject.image.application.fake;

import java.nio.charset.StandardCharsets;

public final class ImageBytes {

    public static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2};
    public static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 0, 3, 4};
    public static final byte[] WEBP = "RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.US_ASCII);
    public static final byte[] GIF = "GIF89a\0\0\0\0\0\0".getBytes(StandardCharsets.US_ASCII);

    private ImageBytes() {
    }
}
