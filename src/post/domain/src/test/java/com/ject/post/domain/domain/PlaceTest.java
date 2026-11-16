package com.ject.post.domain.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlaceTest {

    @ParameterizedTest(name = "{0} → {1}")
    @DisplayName("표시 지역은 마지막 두 단어, 세종은 세종시로 줄인다")
    @CsvSource({
            "서울특별시 마포구 동교동, 마포구 동교동",
            "경기도 파주시 금촌동, 파주시 금촌동",
            "부산광역시 기장군 기장읍, 기장군 기장읍",
            "경기도 수원시 영통구 매탄동, 영통구 매탄동",
            "경기도 양평군 양평읍 양근리, 양평읍 양근리",
            "세종특별자치시 보람동, 세종시 보람동",
            "'  서울특별시   마포구  동교동 ', 마포구 동교동"
    })
    void displayRegionName(String regionName, String expected) {
        Place place = new Place(37.5, 127.0, "장소", regionName, null);

        assertEquals(expected, place.displayRegionName());
    }
}