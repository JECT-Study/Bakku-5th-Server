package com.ject.post.application.service.query;

import com.ject.post.application.dto.HubResult;
import com.ject.post.application.dto.query.GetHubsQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GetHubsHandlerTest {

    @Test
    @DisplayName("고를 수 있는 거점 5곳을 기획 순서대로 중심 좌표 · 다각형과 함께 돌려준다")
    void handle_returnsSelectableHubs() {
        List<HubResult> hubs = new GetHubsHandler().handle(new GetHubsQuery());

        assertEquals(List.of("HONGDAE", "YONGSAN", "SEONGSU", "SINDORIM", "GUKJEON"),
                hubs.stream().map(HubResult::code).toList());
        assertEquals(new HubResult.Coordinate(37.55622, 126.92338), hubs.get(0).center());
        assertEquals(new HubResult.Coordinate(37.55695, 126.92072), hubs.get(0).polygon().get(0));
        hubs.forEach(hub -> assertFalse(hub.polygon().isEmpty(), hub.code()));
    }
}
