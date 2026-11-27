package com.ject.exchange.infrastructure.adapter;

import com.ject.exchange.application.port.ExchangeImageRejectedException;
import com.ject.exchange.application.port.ImagePort;
import com.ject.image.application.contract.ImageAttachment;
import com.ject.image.application.exception.ImageKeyRejectedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ImageAdapter implements ImagePort {

    private static final String PURPOSE = "EXCHANGE";

    private final ImageAttachment imageAttachment;

    @Override
    public List<String> attachToExchange(Long uploaderId, Long exchangeId, List<String> imageKeys) {
        try {
            return imageAttachment.finalizeImages(PURPOSE, uploaderId, exchangeId, imageKeys);
        } catch (ImageKeyRejectedException rejected) {
            throw new ExchangeImageRejectedException(rejected.getOrder(), rejected.getKey());
        }
    }

    @Override
    public void discardFromExchange(Long exchangeId, List<String> imageKeys) {
        imageAttachment.discardImages(PURPOSE, exchangeId, imageKeys);
    }
}
