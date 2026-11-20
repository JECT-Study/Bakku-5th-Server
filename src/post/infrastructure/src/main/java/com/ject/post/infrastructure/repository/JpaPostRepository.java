package com.ject.post.infrastructure.repository;

import com.ject.post.domain.domain.Post;
import com.ject.post.domain.exception.PostNotFoundException;
import com.ject.post.domain.repository.PostRepository;
import com.ject.post.infrastructure.entity.PostEntity;
import com.ject.post.infrastructure.entity.PostImageEntity;
import com.ject.post.infrastructure.entity.PostTagEntity;
import com.ject.post.infrastructure.entity.PostTagLinkEntity;
import com.ject.post.infrastructure.jpa.PostEntityRepository;
import com.ject.post.infrastructure.jpa.PostImageEntityRepository;
import com.ject.post.infrastructure.jpa.PostTagEntityRepository;
import com.ject.post.infrastructure.jpa.PostTagLinkEntityRepository;
import com.ject.post.infrastructure.mapper.PostMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class JpaPostRepository implements PostRepository {

    private final PostEntityRepository postEntityRepository;
    private final PostImageEntityRepository postImageEntityRepository;
    private final PostTagEntityRepository postTagEntityRepository;
    private final PostTagLinkEntityRepository postTagLinkEntityRepository;

    @Override
    @Transactional
    public Post save(Post post) {
        PostEntity entity = post.getId() == null
                ? postEntityRepository.save(PostEntity.from(post))
                : update(post);
        replaceImages(entity.getId(), post.getImageKeys());
        replaceTags(entity.getId(), post.getTags());
        return PostMapper.toDomain(entity, post.getImageKeys(), post.getTags());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Post> findById(Long id) {
        return postEntityRepository.findById(id)
                .map(entity -> PostMapper.toDomain(entity, findImageKeys(id),
                        postTagLinkEntityRepository.findTagNamesByPostId(id)));
    }

    private PostEntity update(Post post) {
        PostEntity entity = postEntityRepository.findById(post.getId())
                .orElseThrow(() -> new PostNotFoundException(post.getId()));
        entity.update(post);
        return entity;
    }

    private void replaceImages(Long postId, List<String> imageKeys) {
        postImageEntityRepository.deleteByPostId(postId);
        List<PostImageEntity> images = new ArrayList<>();
        for (int i = 0; i < imageKeys.size(); i++) {
            images.add(PostImageEntity.builder()
                    .postId(postId)
                    .imageKey(imageKeys.get(i))
                    .sortOrder(i + 1)
                    .build());
        }
        postImageEntityRepository.saveAll(images);
    }

    private void replaceTags(Long postId, List<String> tagNames) {
        postTagLinkEntityRepository.deleteByPostId(postId);
        if (tagNames.isEmpty()) {
            return;
        }
        tagNames.forEach(postTagEntityRepository::insertIfAbsent);
        Map<String, Long> tagIds = postTagEntityRepository.findByNameIn(tagNames).stream()
                .collect(Collectors.toMap(PostTagEntity::getName, PostTagEntity::getId));

        List<PostTagLinkEntity> links = new ArrayList<>();
        for (int i = 0; i < tagNames.size(); i++) {
            links.add(PostTagLinkEntity.builder()
                    .postId(postId)
                    .tagId(tagIds.get(tagNames.get(i)))
                    .sortOrder(i + 1)
                    .build());
        }
        postTagLinkEntityRepository.saveAll(links);
    }

    private List<String> findImageKeys(Long postId) {
        return postImageEntityRepository.findByPostIdOrderBySortOrder(postId).stream()
                .map(PostImageEntity::getImageKey)
                .toList();
    }
}
