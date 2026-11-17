package com.ject.post.domain.domain;

import java.util.Locale;

public final class TagName {

    private TagName() {
    }

    public static String normalize(String name) {
        return name.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }
}
