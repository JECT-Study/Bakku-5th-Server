package com.ject.post.domain.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TagNameTest {

    @Test
    @DisplayName("모든 공백을 지우고 소문자로 바꾼다")
    void normalize() {
        assertEquals("chiikawa", TagName.normalize("  ChiiKawa "));
        assertEquals("치이카와가챠", TagName.normalize("치이카와 가챠"));
        assertEquals("btsjimin", TagName.normalize("BTS  Jimin"));
    }
}
