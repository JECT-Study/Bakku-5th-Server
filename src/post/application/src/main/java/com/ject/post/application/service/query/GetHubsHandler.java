package com.ject.post.application.service.query;

import com.ject.common.core.cqrs.query.QueryHandler;
import com.ject.post.application.dto.HubResult;
import com.ject.post.application.dto.query.GetHubsQuery;
import com.ject.post.domain.domain.Hub;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetHubsHandler implements QueryHandler<GetHubsQuery, List<HubResult>> {

    @Override
    public List<HubResult> handle(GetHubsQuery query) {
        return Hub.selectable().stream()
                .map(hub -> new HubResult(hub.name(), toResult(hub.getCenter()),
                        hub.getPolygon().stream().map(GetHubsHandler::toResult).toList()))
                .toList();
    }

    private static HubResult.Coordinate toResult(Hub.Coordinate coordinate) {
        return new HubResult.Coordinate(coordinate.lat(), coordinate.lng());
    }
}
