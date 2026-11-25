package com.ject.post.domain.exception;

import com.ject.common.core.exception.DomainException;

public class InvalidTagKeywordException extends DomainException {

    public InvalidTagKeywordException(String keyword) {
        super(PostErrorCode.INVALID_TAG_KEYWORD, "태그 검색어는 띄어쓰기와 #을 빼고 1~20자여야 합니다. keyword=" + keyword);
    }
}
