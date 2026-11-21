package com.ject.post.infrastructure.repository;

import com.ject.post.application.port.PostSummaryReader;
import com.ject.post.domain.domain.PostCategory;
import com.ject.post.domain.domain.PostStatus;
import com.ject.post.infrastructure.entity.PostEntity;
import com.ject.post.infrastructure.entity.PostImageEntity;
import com.ject.post.infrastructure.jpa.PostImageEntityRepository;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.ject.post.infrastructure.entity.QPostEntity.postEntity;

@Repository
@RequiredArgsConstructor
public class JpaPostSummaryReader implements PostSummaryReader {

    private static final int THUMBNAIL_SORT_ORDER = 1;

    private final JPAQueryFactory queryFactory;
    private final PostImageEntityRepository postImageEntityRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Row> findHomePage(Condition condition, Long cursorId, int limit) {
        List<PostEntity> posts = queryFactory
                .selectFrom(postEntity)
                .where(
                        postEntity.status.eq(PostStatus.AVAILABLE),
                        exchangeType(condition.nowExchange(), condition.now()),
                        categoryEq(condition.category()),
                        idBefore(cursorId))
                .orderBy(postEntity.id.desc())
                .limit(limit)
                .fetch();
        Map<Long, String> thumbnails = findThumbnails(posts);
        return posts.stream()
                .map(post -> toRow(post, thumbnails.get(post.getId())))
                .toList();
    }

    private static BooleanExpression exchangeType(Boolean nowExchange, Instant now) {
        if (nowExchange == null) {
            return null;
        }
        if (nowExchange) {
            return postEntity.nowExchangeEndsAt.gt(now);
        }
        return postEntity.nowExchangeEndsAt.isNull()
                .or(postEntity.nowExchangeEndsAt.loe(now));
    }

    private static BooleanExpression categoryEq(PostCategory category) {
        return category == null ? null : postEntity.category.eq(category);
    }

    private static BooleanExpression idBefore(Long cursorId) {
        return cursorId == null ? null : postEntity.id.lt(cursorId);
    }

    private Map<Long, String> findThumbnails(List<PostEntity> posts) {
        if (posts.isEmpty()) {
            return Map.of();
        }
        List<Long> postIds = posts.stream().map(PostEntity::getId).toList();
        return postImageEntityRepository.findByPostIdInAndSortOrder(postIds, THUMBNAIL_SORT_ORDER).stream()
                .collect(Collectors.toMap(PostImageEntity::getPostId, PostImageEntity::getImageKey));
    }

    private static Row toRow(PostEntity post, String thumbnailKey) {
        return new Row(
                post.getId(),
                post.getAuthorId(),
                thumbnailKey,
                post.getTitle(),
                post.getPlace().getRegionName(),
                post.getStatus(),
                post.getNowExchangeEndsAt(),
                post.getWantedItem(),
                post.getCreatedAt());
    }
}
