package com.ject.chat.domain.domain.message;

import java.util.List;

/**
 * IMAGE 메시지 내용 (순서를 보존한 사진 URL 1~5개)
 */
public record ImageContent(List<String> imageUrls) implements ChatMessageContent {

    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 5;

    public ImageContent {
        if (imageUrls == null || imageUrls.size() < MIN_SIZE || imageUrls.size() > MAX_SIZE
                || imageUrls.stream().anyMatch(url -> url == null || url.isBlank())) {
            throw new IllegalArgumentException("IMAGE 메시지는 공백이 아닌 URL 1~5개가 필요합니다.");
        }
        imageUrls = List.copyOf(imageUrls);
    }
}
