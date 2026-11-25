package com.ject.post.domain.domain;

import com.ject.post.domain.exception.InvalidSearchKeywordException;

public record SearchKeyword(String value) {

    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 30;

    public static SearchKeyword of(String raw) {
        String value = TagName.normalize(raw).replaceFirst("^#+", "");
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new InvalidSearchKeywordException(raw);
        }
        return new SearchKeyword(value);
    }
}
