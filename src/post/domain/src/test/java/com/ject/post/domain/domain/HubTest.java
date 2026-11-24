package com.ject.post.domain.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class HubTest {

    @ParameterizedTest(name = "{0} ({1}, {2}) → {3}")
    @DisplayName("거점 주요 지점은 그 거점으로 판정한다")
    @CsvSource({
            "홍대 주요거점, 37.55622, 126.92338, HONGDAE",
            "홍대입구역 9번 출구, 37.5560987, 126.9230766, HONGDAE",
            "용산 주요거점, 37.52864, 126.96484, YONGSAN",
            "성수 주요거점, 37.54482, 127.05481, SEONGSU",
            "신도림 주요거점, 37.50892, 126.89136, SINDORIM",
            "국전 주요거점, 37.48463, 127.01836, GUKJEON"
    })
    void of_insideHub(String name, double lat, double lng, Hub expected) {
        assertEquals(expected, Hub.of(lat, lng));
    }

    @ParameterizedTest(name = "{0} ({1}, {2})")
    @DisplayName("어느 다각형에도 없으면 미분류")
    @CsvSource({
            "서울시청, 37.5663, 126.9779",
            "홍대 다각형을 감싼 사각형의 북서쪽 모서리, 37.5582, 126.9199",
            "용산 다각형을 감싼 사각형의 남서쪽 모서리, 37.5263, 126.9621"
    })
    void of_outside(String name, double lat, double lng) {
        assertEquals(Hub.UNCLASSIFIED, Hub.of(lat, lng));
    }

    @Test
    @DisplayName("고를 수 있는 거점은 5곳이고 미분류는 빠진다")
    void selectable_fiveHubs() {
        assertEquals(List.of(Hub.HONGDAE, Hub.YONGSAN, Hub.SEONGSU, Hub.SINDORIM, Hub.GUKJEON), Hub.selectable());
    }

    @Test
    @DisplayName("거점 중심 좌표는 자기 다각형 안에 있다")
    void center_insideOwnPolygon() {
        for (Hub hub : Hub.selectable()) {
            Hub.Coordinate center = hub.getCenter();
            assertEquals(hub, Hub.of(center.lat(), center.lng()), hub.name());
            assertFalse(hub.getPolygon().isEmpty());
        }
    }

    @Test
    @DisplayName("장소 좌표로 거점을 판정한다")
    void place_hub() {
        Place place = new Place(37.5563, 126.9236, "스타벅스 홍대동교점", "서울특별시 마포구 동교동", null);

        assertEquals(Hub.HONGDAE, place.hub());
    }
}
