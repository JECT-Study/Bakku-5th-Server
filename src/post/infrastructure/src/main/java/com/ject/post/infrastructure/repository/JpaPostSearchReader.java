package com.ject.post.infrastructure.repository;

import com.ject.post.application.port.PostSearchReader;
import com.ject.post.domain.domain.PostCategory;
import com.ject.post.domain.domain.PostStatus;
import com.ject.post.infrastructure.entity.PostEntity;
import com.ject.post.infrastructure.entity.PostImageEntity;
import com.ject.post.infrastructure.jpa.PostImageEntityRepository;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.ject.post.infrastructure.entity.QPostEntity.postEntity;
import static com.ject.post.infrastructure.entity.QPostTagEntity.postTagEntity;
import static com.ject.post.infrastructure.entity.QPostTagLinkEntity.postTagLinkEntity;

@Repository
@RequiredArgsConstructor
public class JpaPostSearchReader implements PostSearchReader {

    private final JPAQueryFactory queryFactory;
    private final PostImageEntityRepository postImageEntityRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Hit> search(Condition condition, Cursor cursor, int limit) {
        String keyword = condition.keyword();
        BooleanExpression titleContains = postEntity.titleNormalized.contains(keyword);
        BooleanExpression tagExact = tagExists(postTagEntity.name.eq(keyword));
        BooleanExpression tagContains = tagExists(postTagEntity.name.contains(keyword));
        NumberExpression<Integer> rank = new CaseBuilder()
                .when(tagExact).then(1)
                .when(titleContains).then(2)
                .otherwise(3);

        List<Tuple> tuples = queryFactory
                .select(postEntity, rank)
                .from(postEntity)
                .where(
                        postEntity.status.eq(PostStatus.AVAILABLE),
                        categoryEq(condition.category()),
                        titleContains.or(tagContains),
                        after(rank, cursor))
                .orderBy(rank.asc(), postEntity.id.desc())
                .limit(limit)
                .fetch();

        List<PostEntity> posts = tuples.stream().map(tuple -> tuple.get(postEntity)).toList();
        Map<Long, String> thumbnails = findThumbnails(posts);
        return tuples.stream()
                .map(tuple -> {
                    PostEntity post = tuple.get(postEntity);
                    return new Hit(tuple.get(rank), JpaPostSummaryReader.toRow(post, thumbnails.get(post.getId())));
                })
                .toList();
    }

    private static BooleanExpression tagExists(BooleanExpression tagCondition) {
        return JPAExpressions.selectOne()
                .from(postTagLinkEntity, postTagEntity)
                .where(
                        postTagLinkEntity.postId.eq(postEntity.id),
                        postTagLinkEntity.tagId.eq(postTagEntity.id),
                        tagCondition)
                .exists();
    }

    private static BooleanExpression categoryEq(PostCategory category) {
        return category == null ? null : postEntity.category.eq(category);
    }

    private static BooleanExpression after(NumberExpression<Integer> rank, Cursor cursor) {
        if (cursor == null) {
            return null;
        }
        return rank.gt(cursor.rank())
                .or(rank.eq(cursor.rank()).and(postEntity.id.lt(cursor.id())));
    }

    private Map<Long, String> findThumbnails(List<PostEntity> posts) {
        if (posts.isEmpty()) {
            return Map.of();
        }
        List<Long> postIds = posts.stream().map(PostEntity::getId).toList();
        return postImageEntityRepository
                .findByPostIdInAndSortOrder(postIds, JpaPostSummaryReader.THUMBNAIL_SORT_ORDER).stream()
                .collect(Collectors.toMap(PostImageEntity::getPostId, PostImageEntity::getImageKey));
    }
}
