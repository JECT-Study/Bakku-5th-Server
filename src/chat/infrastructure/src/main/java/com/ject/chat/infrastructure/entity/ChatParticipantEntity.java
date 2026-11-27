package com.ject.chat.infrastructure.entity;

import com.ject.chat.domain.domain.chatroom.ChatParticipantRole;
import com.ject.common.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Entity
@Getter
@Table(name = "chat_participant")
public class ChatParticipantEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoomEntity chatRoom;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private ChatParticipantRole role;

    @Column(name = "left_at")
    private Instant leftAt;

    protected ChatParticipantEntity() {}

    @Builder
    public ChatParticipantEntity(Long id, ChatRoomEntity chatRoom, Long memberId, ChatParticipantRole role, Instant leftAt) {
        this.id = id;
        this.chatRoom = chatRoom;
        this.memberId = memberId;
        this.role = role;
        this.leftAt = leftAt;
    }
}
