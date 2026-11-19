package com.ject.post.infrastructure.jpa;

import com.ject.post.domain.domain.PostCategory;
import com.ject.post.domain.domain.PostStatus;
import com.ject.post.infrastructure.entity.PostEntity;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;

public final class PostHomeSpecifications {

    private static final List<PostStatus> HOME_STATUSES = List.of(PostStatus.AVAILABLE, PostStatus.RESERVED);

    private PostHomeSpecifications() {
    }

    public static Specification<PostEntity> shownOnHome() {
        return (root, query, cb) -> root.get("status").in(HOME_STATUSES);
    }

    public static Specification<PostEntity> nowExchange(Instant now) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), PostStatus.AVAILABLE),
                cb.greaterThan(root.<Instant>get("nowExchangeEndsAt"), now));
    }

    public static Specification<PostEntity> normalExchange(Instant now) {
        return (root, query, cb) -> cb.or(
                cb.notEqual(root.get("status"), PostStatus.AVAILABLE),
                cb.isNull(root.get("nowExchangeEndsAt")),
                cb.lessThanOrEqualTo(root.<Instant>get("nowExchangeEndsAt"), now));
    }

    public static Specification<PostEntity> category(PostCategory category) {
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    public static Specification<PostEntity> idBefore(Long cursorId) {
        return (root, query, cb) -> cb.lessThan(root.<Long>get("id"), cursorId);
    }
}
