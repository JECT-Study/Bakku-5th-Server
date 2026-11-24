package com.ject.notification.api.controller;

import com.ject.notification.api.exception.InvalidCursorException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 알림 목록 커서. 교환글 · 차단 목록과 같은 형식(Base64 URL `{"id":N}`)이다.
 */
final class NotificationCursor {

    private static final Pattern FORMAT = Pattern.compile("^\\{\"id\":(\\d{1,18})}$");

    private NotificationCursor() {
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
