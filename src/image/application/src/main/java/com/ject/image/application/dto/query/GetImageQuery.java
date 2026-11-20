package com.ject.image.application.dto.query;

import com.ject.common.core.cqrs.query.Query;
import com.ject.image.application.dto.StoredImage;

public record GetImageQuery(String key, Long viewerId) implements Query<StoredImage> {
}
