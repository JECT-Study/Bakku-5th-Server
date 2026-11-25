package com.ject.post.domain.exception;

import com.ject.common.core.exception.DomainException;

public class InvalidSearchKeywordException extends DomainException {

    public InvalidSearchKeywordException(String keyword) {
        super(PostErrorCode.INVALID_KEYWORD, "검색어는 띄어쓰기와 #을 빼고 1~30자여야 합니다. keyword=" + keyword);
    }
}
