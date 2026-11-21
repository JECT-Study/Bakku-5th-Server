package com.ject.post.api.controller;

import com.ject.post.api.exception.InvalidCursorException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PostCursor {

    private static final Pattern FORMAT = Pattern.compile("^\\{\"id\":(\\d{1,18})}$");

    private PostCursor() {
    }

    static String encode(Long id) {
        if (id == null) {
            return null;
        }
        String json = "{\"id\":" + id + "}";
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    static Long decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String json = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            Matcher matcher = FORMAT.matcher(json);
            if (matcher.matches()) {
                return Long.valueOf(matcher.group(1));
            }
        } catch (IllegalArgumentException e) {
            throw new InvalidCursorException(cursor);
        }
        throw new InvalidCursorException(cursor);
    }
}
