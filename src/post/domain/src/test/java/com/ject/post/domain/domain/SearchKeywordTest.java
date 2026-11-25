package com.ject.post.domain.domain;

import com.ject.post.domain.exception.InvalidSearchKeywordException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SearchKeywordTest {

    @ParameterizedTest(name = "\"{0}\" → \"{1}\"")
    @DisplayName("모든 공백과 앞의 #을 지우고 소문자로 바꾼다")
    @CsvSource(delimiter = '|', value = {
            "치이카와|치이카와",
            "'  치이 카와  '|치이카와",
            "#치이카와|치이카와",
            "'# 치이 카와'|치이카와",
            "##치이카와|치이카와",
            "Chii KAWA|chiikawa",
            "치이#카와|치이#카와"
    })
    void of_normalizes(String raw, String expected) {
        assertEquals(expected, SearchKeyword.of(raw).value());
    }

    @Test
    @DisplayName("정규화한 뒤 1자와 30자는 된다")
    void of_boundary() {
        assertEquals(1, SearchKeyword.of("# 가").value().length());
        assertEquals(30, SearchKeyword.of("가".repeat(30)).value().length());
    }

    @ParameterizedTest(name = "\"{0}\"")
    @DisplayName("정규화한 뒤 1~30자가 아니면 POST_009")
    @ValueSource(strings = {"#", "##", "   ", "# ", "가나다라마바사아자차카타파하가나다라마바사아자차카타파하가나다"})
    void of_invalidLength_throws(String raw) {
        InvalidSearchKeywordException e = assertThrows(InvalidSearchKeywordException.class, () -> SearchKeyword.of(raw));

        assertEquals("POST_009", e.getErrorCode().getCode());
    }
}
