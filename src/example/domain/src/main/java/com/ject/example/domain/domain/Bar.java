package com.ject.example.domain.domain;

import com.ject.example.domain.exception.InvalidBarContentException;

/**
 * Foo Aggregate 내부의 엔티티. 새 Bar는 Foo를 통해서만 만들 수 있다.
 */
public class Bar {

    private static final int MAX_CONTENT_LENGTH = 100;

    private final Long id;
    private final String content;

    private Bar(Long id, String content) {
        if (content == null || content.isBlank()) {
            throw new InvalidBarContentException("Bar 내용은 비어 있을 수 없습니다.");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new InvalidBarContentException("Bar 내용은 " + MAX_CONTENT_LENGTH + "자를 넘을 수 없습니다.");
        }
        this.id = id;
        this.content = content;
    }

    // package-private: 같은 패키지의 Foo만 새 Bar를 만들 수 있다
    static Bar create(String content) {
        return new Bar(null, content);
    }

    // 저장소에서 읽어온 데이터로 복원할 때 사용
    public static Bar restore(Long id, String content) {
        return new Bar(id, content);
    }

    public Long getId() {
        return id;
    }

    public String getContent() {
        return content;
    }
}
