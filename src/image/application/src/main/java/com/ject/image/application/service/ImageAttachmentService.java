package com.ject.image.application.service;

import com.ject.image.application.contract.ImageAttachment;
import com.ject.image.application.exception.ImageKeyRejectedException;
import com.ject.image.application.port.ImageStorage;
import com.ject.image.domain.domain.ImageKey;
import com.ject.image.domain.domain.ImagePurpose;
import com.ject.image.domain.exception.ImageNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ImageAttachmentService implements ImageAttachment {

    private final ImageStorage imageStorage;

    @Override
    public List<String> finalizeImages(String purpose, Long uploaderId, Long targetId, List<String> keys) {
        ImagePurpose expected = ImagePurpose.valueOf(purpose);
        List<ImageKey> parsed = new ArrayList<>();
        for (int i = 0; i < keys.size(); i++) {
            parsed.add(accept(i + 1, keys.get(i), expected, uploaderId, targetId));
        }

        List<String> finalKeys = new ArrayList<>();
        for (ImageKey key : parsed) {
            if (key.isTmp()) {
                ImageKey finalKey = key.toFinal(targetId);
                imageStorage.copy(key.value(), finalKey.value());
                finalKeys.add(finalKey.value());
            } else {
                finalKeys.add(key.value());
            }
        }
        return finalKeys;
    }

    @Override
    public void discardImages(String purpose, Long targetId, List<String> keys) {
        ImagePurpose expected = ImagePurpose.valueOf(purpose);
        for (String raw : keys) {
            tryParse(raw)
                    .filter(key -> !key.isTmp())
                    .filter(key -> key.purpose() == expected && targetId.equals(key.targetId()))
                    .ifPresent(key -> imageStorage.delete(key.value()));
        }
    }

    private ImageKey accept(int order, String raw, ImagePurpose expected, Long uploaderId, Long targetId) {
        ImageKey key = tryParse(raw).orElseThrow(() -> new ImageKeyRejectedException(order, raw));
        boolean accepted = key.purpose() == expected
                && (key.isTmp()
                ? uploaderId.equals(key.uploaderId()) && imageStorage.exists(raw)
                : targetId.equals(key.targetId()));
        if (!accepted) {
            throw new ImageKeyRejectedException(order, raw);
        }
        return key;
    }

    private static Optional<ImageKey> tryParse(String raw) {
        try {
            return Optional.of(ImageKey.parse(raw));
        } catch (ImageNotFoundException e) {
            return Optional.empty();
        }
    }
}
