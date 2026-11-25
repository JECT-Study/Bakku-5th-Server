package com.ject.post.domain.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PlaceTest {

    @ParameterizedTest(name = "\"{0}\"")
    @NullSource
    @ValueSource(strings = {"", " ", "   "})
    @DisplayName("상세 주소가 비었거나 공백뿐이면 없음(null)")
    void memo_blank_isNull(String memo) {
        assertNull(place(memo).memo());
    }

    @Test
    @DisplayName("상세 주소가 있으면 그대로 둔다")
    void memo_present_kept() {
        assertEquals(" AK플라자 주변", place(" AK플라자 주변").memo());
    }

    private static Place place(String memo) {
        return new Place(37.5563, 126.9236, "스타벅스 홍대동교점", "서울특별시 마포구 동교동", memo);
    }
}
