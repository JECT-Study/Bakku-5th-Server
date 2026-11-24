package com.ject.post.application.dto.query;

import com.ject.common.core.cqrs.query.Query;
import com.ject.post.application.dto.HubResult;

import java.util.List;

public record GetHubsQuery() implements Query<List<HubResult>> {
}
