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
@Table(name = "post_tag_link")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostTagLinkEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "tag_id", nullable = false)
    private Long tagId;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Builder
    public PostTagLinkEntity(Long postId, Long tagId, int sortOrder) {
        this.postId = postId;
        this.tagId = tagId;
        this.sortOrder = sortOrder;
    }
}
