package com.ject.post.infrastructure.repository;

import com.ject.post.application.port.PostSearchReader;
import com.ject.post.domain.domain.PostStatus;
import com.ject.post.infrastructure.entity.PostEntity;
import com.ject.post.infrastructure.entity.PostImageEntity;
import com.ject.post.infrastructure.jpa.PostImageEntityRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.ject.post.infrastructure.entity.QPostEntity.postEntity;

@Repository
@RequiredArgsConstructor
public class JpaPostSearchReader implements PostSearchReader {

    private static final String AVAILABLE = PostStatus.AVAILABLE.name();

    private static final String RANKED_HITS = """
            SELECT c.id, c.rank
            FROM (SELECT hit.id, min(hit.rank) AS rank
                  FROM (SELECT p.id, 2 AS rank
                        FROM post p
                        WHERE p.title_normalized LIKE :pattern ESCAPE '!' AND %1$s
                        UNION ALL
                        SELECT l.post_id, CASE WHEN t.name = :keyword THEN 1 ELSE 3 END
                        FROM post_tag t
                        JOIN post_tag_link l ON l.tag_id = t.id
                        JOIN post p ON p.id = l.post_id AND %1$s
                        WHERE t.name LIKE :pattern ESCAPE '!') hit
                  GROUP BY hit.id) c
            """;

    private final EntityManager entityManager;
    private final JPAQueryFactory queryFactory;
    private final PostImageEntityRepository postImageEntityRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Hit> search(Condition condition, Cursor cursor, int limit) {
        List<Object[]> rankedIds = rankedIds(condition, cursor, limit);
        if (rankedIds.isEmpty()) {
            return List.of();
        }

        List<Long> ids = rankedIds.stream().map(row -> ((Number) row[0]).longValue()).toList();
        Map<Long, PostEntity> posts = queryFactory
                .selectFrom(postEntity)
                .where(postEntity.id.in(ids))
                .fetch().stream()
                .collect(Collectors.toMap(PostEntity::getId, Function.identity()));
        Map<Long, String> thumbnails = findThumbnails(ids);

        return rankedIds.stream()
                .map(row -> {
                    Long id = ((Number) row[0]).longValue();
                    int rank = ((Number) row[1]).intValue();
                    return new Hit(rank, JpaPostSummaryReader.toRow(posts.get(id), thumbnails.get(id)));
                })
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> rankedIds(Condition condition, Cursor cursor, int limit) {
        List<String> postFilters = new ArrayList<>();
        postFilters.add("p.status = '" + AVAILABLE + "'");
        if (condition.category() != null) {
            postFilters.add("p.category = :category");
        }
        if (condition.hub() != null) {
            postFilters.add("p.hub = :hub");
        }
        if (Boolean.TRUE.equals(condition.nowExchange())) {
            postFilters.add("p.now_exchange_ends_at > :now");
        }
        if (Boolean.FALSE.equals(condition.nowExchange())) {
            postFilters.add("(p.now_exchange_ends_at IS NULL OR p.now_exchange_ends_at <= :now)");
        }
        boolean excludesAuthors = condition.excludedAuthorIds() != null && !condition.excludedAuthorIds().isEmpty();
        if (excludesAuthors) {
            postFilters.add("p.author_id NOT IN (:excludedAuthorIds)");
        }

        StringBuilder sql = new StringBuilder(RANKED_HITS.formatted(String.join(" AND ", postFilters)));
        if (cursor != null) {
            sql.append("WHERE c.rank > :cursorRank OR (c.rank = :cursorRank AND c.id < :cursorId)\n");
        }
        sql.append("ORDER BY c.rank, c.id DESC\nLIMIT :limit");

        Query query = entityManager.createNativeQuery(sql.toString())
                .setParameter("pattern", containsPattern(condition.keyword()))
                .setParameter("keyword", condition.keyword())
                .setParameter("limit", limit);
        if (condition.category() != null) {
            query.setParameter("category", condition.category().name());
        }
        if (condition.hub() != null) {
            query.setParameter("hub", condition.hub().name());
        }
        if (condition.nowExchange() != null) {
            query.setParameter("now", condition.now());
        }
        if (excludesAuthors) {
            query.setParameter("excludedAuthorIds", condition.excludedAuthorIds());
        }
        if (cursor != null) {
            query.setParameter("cursorRank", cursor.rank());
            query.setParameter("cursorId", cursor.id());
        }
        return query.getResultList();
    }

    private static String containsPattern(String keyword) {
        String escaped = keyword
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escaped + "%";
    }

    private Map<Long, String> findThumbnails(List<Long> postIds) {
        return postImageEntityRepository
                .findByPostIdInAndSortOrder(postIds, JpaPostSummaryReader.THUMBNAIL_SORT_ORDER).stream()
                .collect(Collectors.toMap(PostImageEntity::getPostId, PostImageEntity::getImageKey));
    }
}
