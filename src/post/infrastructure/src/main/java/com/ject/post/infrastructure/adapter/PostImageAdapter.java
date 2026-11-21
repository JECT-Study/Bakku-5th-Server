package com.ject.post.infrastructure.adapter;

import com.ject.image.application.contract.ImageAttachment;
import com.ject.image.application.exception.ImageKeyRejectedException;
import com.ject.post.application.port.PostImagePort;
import com.ject.post.domain.exception.InvalidImageKeyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PostImageAdapter implements PostImagePort {

    private static final String PURPOSE = "POST";

    private final ImageAttachment imageAttachment;

    @Override
    public List<String> attachToPost(Long authorId, Long postId, List<String> imageKeys) {
        try {
            return imageAttachment.finalizeImages(PURPOSE, authorId, postId, imageKeys);
        } catch (ImageKeyRejectedException e) {
            throw new InvalidImageKeyException(e.getOrder(), e.getKey());
        }
    }
}
