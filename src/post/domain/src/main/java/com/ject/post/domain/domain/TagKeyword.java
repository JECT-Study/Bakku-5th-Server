package com.ject.post.domain.domain;

import com.ject.post.domain.exception.InvalidTagKeywordException;

public record TagKeyword(String value) {

    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 20;

    public static TagKeyword of(String raw) {
        String value = TagName.normalize(raw).replaceFirst("^#+", "");
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new InvalidTagKeywordException(raw);
        }
        return new TagKeyword(value);
    }
}
