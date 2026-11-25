package com.ject.post.infrastructure.repository;

import com.ject.post.application.port.TagSuggestionReader;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.ject.post.infrastructure.entity.QPostTagEntity.postTagEntity;

@Repository
@RequiredArgsConstructor
public class JpaTagSuggestionReader implements TagSuggestionReader {

    private final JPAQueryFactory queryFactory;

    @Override
    @Transactional(readOnly = true)
    public List<String> suggest(String keyword, int limit) {
        StringExpression nameInCodePointOrder = Expressions.stringTemplate("collate({0} as ucs_basic)", postTagEntity.name);
        return queryFactory
                .select(postTagEntity.name)
                .from(postTagEntity)
                .where(
                        postTagEntity.name.contains(keyword),
                        postTagEntity.postCount.gt(0))
                .orderBy(postTagEntity.postCount.desc(), nameInCodePointOrder.asc())
                .limit(limit)
                .fetch();
    }
}
