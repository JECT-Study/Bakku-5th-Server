package com.ject.chat.infrastructure.entity;

import com.ject.common.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;

@Entity
@Getter
@Table(name = "chat_room")
public class ChatRoomEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exchange_id", nullable = false)
    private Long exchangeId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    protected ChatRoomEntity() {}

    @Builder
    public ChatRoomEntity(Long id, Long exchangeId, Long postId, Long requesterId) {
        this.id = id;
        this.exchangeId = exchangeId;
        this.postId = postId;
        this.requesterId = requesterId;
    }
}
