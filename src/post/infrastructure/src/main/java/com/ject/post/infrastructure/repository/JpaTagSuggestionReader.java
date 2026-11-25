package com.ject.post.infrastructure.repository;

import com.ject.post.application.port.TagSuggestionReader;
import com.ject.post.domain.domain.PostStatus;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.ject.post.infrastructure.entity.QPostEntity.postEntity;
import static com.ject.post.infrastructure.entity.QPostTagEntity.postTagEntity;
import static com.ject.post.infrastructure.entity.QPostTagLinkEntity.postTagLinkEntity;

@Repository
@RequiredArgsConstructor
public class JpaTagSuggestionReader implements TagSuggestionReader {

    private final JPAQueryFactory queryFactory;

    @Override
    @Transactional(readOnly = true)
    public List<String> suggest(String keyword, int limit) {
        NumberExpression<Long> postCount = postTagLinkEntity.count();
        StringExpression nameInCodePointOrder = Expressions.stringTemplate("collate({0} as ucs_basic)", postTagEntity.name);
        return queryFactory
                .select(postTagEntity.name)
                .from(postTagEntity)
                .join(postTagLinkEntity).on(postTagLinkEntity.tagId.eq(postTagEntity.id))
                .join(postEntity).on(postEntity.id.eq(postTagLinkEntity.postId))
                .where(
                        postTagEntity.name.contains(keyword),
                        postEntity.status.ne(PostStatus.DELETED))
                .groupBy(postTagEntity.id, postTagEntity.name)
                .orderBy(postCount.desc(), nameInCodePointOrder.asc())
                .limit(limit)
                .fetch();
    }
}
