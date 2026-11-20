package com.ject.image.application.service.query;

import com.ject.common.core.cqrs.query.QueryHandler;
import com.ject.image.application.dto.StoredImage;
import com.ject.image.application.dto.query.GetImageQuery;
import com.ject.image.application.port.ImageStorage;
import com.ject.image.domain.domain.ImageKey;
import com.ject.image.domain.exception.ImageAccessDeniedException;
import com.ject.image.domain.exception.ImageNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetImageHandler implements QueryHandler<GetImageQuery, StoredImage> {

    private final ImageStorage imageStorage;

    @Override
    public StoredImage handle(GetImageQuery query) {
        ImageKey key = ImageKey.parse(query.key());
        if (!key.canBeViewedBy(query.viewerId())) {
            throw new ImageAccessDeniedException(query.key());
        }
        return imageStorage.get(key.value())
                .orElseThrow(() -> new ImageNotFoundException(query.key()));
    }
}
