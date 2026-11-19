package com.ject.post.infrastructure.repository;

import com.ject.post.application.port.PostSummaryReader;
import com.ject.post.infrastructure.entity.PostEntity;
import com.ject.post.infrastructure.entity.PostImageEntity;
import com.ject.post.infrastructure.jpa.PostEntityRepository;
import com.ject.post.infrastructure.jpa.PostHomeSpecifications;
import com.ject.post.infrastructure.jpa.PostImageEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class JpaPostSummaryReader implements PostSummaryReader {

    private static final int THUMBNAIL_SORT_ORDER = 1;

    private final PostEntityRepository postEntityRepository;
    private final PostImageEntityRepository postImageEntityRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Row> findHomePage(Condition condition, Long cursorId, int limit) {
        List<PostEntity> posts = postEntityRepository.findBy(homeSpecification(condition, cursorId),
                query -> query.sortBy(Sort.by(Sort.Direction.DESC, "id")).limit(limit).all());
        Map<Long, String> thumbnails = findThumbnails(posts);
        return posts.stream()
                .map(post -> toRow(post, thumbnails.get(post.getId())))
                .toList();
    }

    private static Specification<PostEntity> homeSpecification(Condition condition, Long cursorId) {
        List<Specification<PostEntity>> specifications = new ArrayList<>();
        specifications.add(PostHomeSpecifications.shownOnHome());
        if (Boolean.TRUE.equals(condition.nowExchange())) {
            specifications.add(PostHomeSpecifications.nowExchange(condition.now()));
        } else if (Boolean.FALSE.equals(condition.nowExchange())) {
            specifications.add(PostHomeSpecifications.normalExchange(condition.now()));
        }
        if (condition.category() != null) {
            specifications.add(PostHomeSpecifications.category(condition.category()));
        }
        if (cursorId != null) {
            specifications.add(PostHomeSpecifications.idBefore(cursorId));
        }
        return Specification.allOf(specifications);
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
