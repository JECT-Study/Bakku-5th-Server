package com.ject.post.infrastructure.entity;

import com.ject.common.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "post_image")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostImageEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "image_key", nullable = false)
    private String imageKey;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Builder
    public PostImageEntity(Long postId, String imageKey, int sortOrder) {
        this.postId = postId;
        this.imageKey = imageKey;
        this.sortOrder = sortOrder;
    }
}
