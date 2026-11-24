package com.ject.post.api.controller.api;

import com.ject.common.web.auth.LoginMember;
import com.ject.common.web.response.ApiResponse;
import com.ject.post.api.dto.response.HubsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Hub", description = "거점 목록")
@RequestMapping("/api/v1/hubs")
public interface HubApi {

    @Operation(summary = "PO-12 거점 목록", description = "홈 거점 선택 · 게시글 작성 지도의 거점 칩과 거점 표시에 쓰는 거점 5곳의 이름 · 중심 좌표 · 다각형을 보여줍니다. 게시글의 거점은 게시할 때 서버가 같은 다각형으로 정합니다.")
    @GetMapping
    ApiResponse<HubsResponse> getHubs(@LoginMember Long memberId);
}
