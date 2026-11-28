package com.ject.chat.infrastructure.entity;

import com.ject.chat.domain.domain.message.CardType;
import com.ject.chat.domain.domain.message.ChatMessageType;
import com.ject.chat.domain.domain.message.SystemLinkType;
import com.ject.chat.domain.domain.message.SystemMessageCode;
import com.ject.common.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;

@Entity
@Getter
@Table(name = "chat_message")
public class ChatMessageEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_room_id", nullable = false)
    private Long chatRoomId;

    @Column(name = "sender_user_id")
    private Long senderUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private ChatMessageType type;

    @Column(name = "text_content", length = 1000)
    private String textContent;

    @Column(name = "external_trade_detected")
    private Boolean externalTradeDetected;

    @Enumerated(EnumType.STRING)
    @Column(name = "system_code", length = 50)
    private SystemMessageCode systemCode;

    @Column(name = "system_text", length = 1000)
    private String systemText;

    @Enumerated(EnumType.STRING)
    @Column(name = "system_link_type", length = 30)
    private SystemLinkType systemLinkType;

    @Column(name = "system_link_id")
    private Long systemLinkId;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", length = 50)
    private CardType cardType;

    @Column(name = "card_target_id")
    private Long cardTargetId;

    @Column(name = "client_message_id", length = 36)
    private String clientMessageId;

    protected ChatMessageEntity() {}

    @Builder
    public ChatMessageEntity(Long id, Long chatRoomId, Long senderUserId, ChatMessageType type, String textContent,
                              Boolean externalTradeDetected, SystemMessageCode systemCode, String systemText,
                              SystemLinkType systemLinkType, Long systemLinkId, CardType cardType, Long cardTargetId,
                              String clientMessageId) {
        this.id = id;
        this.chatRoomId = chatRoomId;
        this.senderUserId = senderUserId;
        this.type = type;
        this.textContent = textContent;
        this.externalTradeDetected = externalTradeDetected;
        this.systemCode = systemCode;
        this.systemText = systemText;
        this.systemLinkType = systemLinkType;
        this.systemLinkId = systemLinkId;
        this.cardType = cardType;
        this.cardTargetId = cardTargetId;
        this.clientMessageId = clientMessageId;
    }
}
