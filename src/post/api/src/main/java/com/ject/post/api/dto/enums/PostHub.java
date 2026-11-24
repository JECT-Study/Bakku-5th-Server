package com.ject.post.api.dto.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PostHub {
    HONGDAE("홍대"),
    YONGSAN("용산"),
    SEONGSU("성수"),
    SINDORIM("신도림"),
    GUKJEON("국제전자센터");

    private final String displayName;
}
