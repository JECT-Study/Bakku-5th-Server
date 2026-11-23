package com.ject.post.infrastructure.entity;

import com.ject.common.jpa.BaseEntity;
import com.ject.post.domain.domain.Hub;
import com.ject.post.domain.domain.Post;
import com.ject.post.domain.domain.PostCategory;
import com.ject.post.domain.domain.PostStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@Table(name = "post")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PostStatus status;

    @Column(name = "title", nullable = false, length = 40)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private PostCategory category;

    @Column(name = "is_opened", nullable = false)
    private boolean opened;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "wanted_item", length = 50)
    private String wantedItem;

    @Embedded
    private PlaceEmbeddable place;

    @Enumerated(EnumType.STRING)
    @Column(name = "hub", nullable = false, length = 20)
    private Hub hub;

    @Column(name = "now_exchange_ends_at")
    private Instant nowExchangeEndsAt;

    @Column(name = "view_count", nullable = false)
    private int viewCount;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public static PostEntity from(Post post) {
        PostEntity entity = new PostEntity();
        entity.authorId = post.getAuthorId();
        entity.update(post);
        return entity;
    }

    public void update(Post post) {
        this.status = post.getStatus();
        this.title = post.getTitle();
        this.category = post.getCategory();
        this.opened = post.isOpened();
        this.description = post.getDescription();
        this.wantedItem = post.getWantedItem();
        this.place = PlaceEmbeddable.builder()
                .lat(post.getPlace().lat())
                .lng(post.getPlace().lng())
                .name(post.getPlace().name())
                .regionName(post.getPlace().regionName())
                .memo(post.getPlace().memo())
                .build();
        this.hub = post.getPlace().hub();
        this.nowExchangeEndsAt = post.getNowExchangeEndsAt();
        this.viewCount = post.getViewCount();
        this.deletedAt = post.getDeletedAt();
    }

    public void changeStatus(PostStatus status) {
        this.status = status;
    }

    public void markDeleted(Instant deletedAt) {
        this.status = PostStatus.DELETED;
        this.deletedAt = deletedAt;
    }
}
