package com.ject.post.api.controller;

import com.ject.post.api.exception.InvalidCursorException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

record PostSearchCursor(int rank, long id) {

    private static final Pattern FORMAT = Pattern.compile("^\\{\"rank\":([1-3]),\"id\":(\\d{1,18})}$");

    static String encode(Integer rank, Long id) {
        if (rank == null || id == null) {
            return null;
        }
        String json = "{\"rank\":" + rank + ",\"id\":" + id + "}";
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    static PostSearchCursor decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String json = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            Matcher matcher = FORMAT.matcher(json);
            if (matcher.matches()) {
                return new PostSearchCursor(Integer.parseInt(matcher.group(1)), Long.parseLong(matcher.group(2)));
            }
        } catch (IllegalArgumentException e) {
            throw new InvalidCursorException(cursor);
        }
        throw new InvalidCursorException(cursor);
    }
}
