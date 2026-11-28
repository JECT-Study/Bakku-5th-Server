package com.ject.chat.infrastructure.entity;

import com.ject.common.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;

@Entity
@Getter
@Table(name = "chat_message_image")
public class ChatMessageImageEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private ChatMessageEntity message;

    @Column(name = "image_url", nullable = false, length = 2048)
    private String imageUrl;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected ChatMessageImageEntity() {}

    @Builder
    public ChatMessageImageEntity(Long id, ChatMessageEntity message, String imageUrl, int sortOrder) {
        this.id = id;
        this.message = message;
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder;
    }
}
