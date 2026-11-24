package com.ject.post.api.controller;

import com.ject.common.core.cqrs.query.QueryBus;
import com.ject.common.web.response.ApiResponse;
import com.ject.post.api.controller.api.HubApi;
import com.ject.post.api.dto.response.HubsResponse;
import com.ject.post.application.dto.query.GetHubsQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HubController implements HubApi {

    private final QueryBus queryBus;

    @Override
    public ApiResponse<HubsResponse> getHubs(Long memberId) {
        return ApiResponse.success(HubsResponse.from(queryBus.send(new GetHubsQuery())));
    }
}
