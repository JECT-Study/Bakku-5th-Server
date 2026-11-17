package com.ject.image.domain.domain;

import java.util.Locale;

public enum ImagePurpose {
    POST,
    EXCHANGE,
    CHAT;

    public String prefix() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static ImagePurpose fromPrefix(String prefix) {
        return valueOf(prefix.toUpperCase(Locale.ROOT));
    }
}
