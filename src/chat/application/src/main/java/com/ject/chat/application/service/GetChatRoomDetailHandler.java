package com.ject.chat.application.service;

import com.ject.chat.application.dto.ChatAppointmentState;
import com.ject.chat.application.dto.ChatExchangeState;
import com.ject.chat.application.dto.ChatInputDisabledReason;
import com.ject.chat.application.dto.ChatMemberInfo;
import com.ject.chat.application.dto.ChatMemberRestriction;
import com.ject.chat.application.dto.ChatRoomAccessStatus;
import com.ject.chat.application.dto.ChatRoomDetailResult;
import com.ject.chat.application.dto.ChatRoomLeaveImpact;
import com.ject.chat.application.dto.CounterpartDetail;
import com.ject.chat.application.dto.query.GetChatRoomDetailQuery;
import com.ject.chat.application.port.ChatExchangePort;
import com.ject.chat.application.port.ChatMemberPort;
import com.ject.chat.domain.domain.chatroom.ChatParticipant;
import com.ject.chat.domain.domain.chatroom.ChatRoom;
import com.ject.chat.domain.exception.ChatErrorCode;
import com.ject.chat.domain.exception.ChatException;
import com.ject.chat.domain.repository.ChatRoomRepository;
import com.ject.common.core.cqrs.query.QueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetChatRoomDetailHandler implements QueryHandler<GetChatRoomDetailQuery, ChatRoomDetailResult> {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberPort chatMemberPort;
    private final ChatExchangePort chatExchangePort;

    @Override
    @Transactional(readOnly = true)
    public ChatRoomDetailResult handle(GetChatRoomDetailQuery query) {
        ChatRoom chatRoom = chatRoomRepository.findById(query.chatRoomId())
                .orElseThrow(() -> new ChatException(ChatErrorCode.CHAT_ROOM_NOT_FOUND));
        ChatParticipant viewer = chatRoom.currentParticipantOf(query.viewerId());
        ChatParticipant counterpart = chatRoom.counterpartOf(query.viewerId());

        ChatMemberInfo counterpartInfo = chatMemberPort.getSummary(counterpart.getMemberId());
        boolean blocked = chatMemberPort.isBlocked(viewer.getMemberId(), counterpart.getMemberId());
        boolean counterpartRestricted = chatMemberPort.getRestriction(counterpart.getMemberId()).restricted();
        boolean viewerRestricted = chatMemberPort.getRestriction(viewer.getMemberId()).restricted();
        ChatExchangeState exchangeState = chatExchangePort.getExchangeState(chatRoom.getExchangeId(), query.viewerId());

        ChatInputDisabledReason reason = resolveInputDisabledReason(blocked, counterpartInfo.withdrawn(),
                counterpartRestricted, viewerRestricted, counterpart.hasLeft(), exchangeState.otherExchangeCompleted());
        ChatRoomAccessStatus status = reason == null ? ChatRoomAccessStatus.OPEN : ChatRoomAccessStatus.READ_ONLY;

        CounterpartDetail counterpartDetail = new CounterpartDetail(counterpartInfo.userId(), counterpartInfo.nickname(),
                counterpartInfo.completedCount(), counterpart.hasLeft());

        return new ChatRoomDetailResult(
                chatRoom.getId(),
                chatRoom.getExchangeId(),
                status,
                reason,
                counterpartDetail,
                viewer.isNotificationEnabled(),
                leaveImpactOf(exchangeState.appointmentState()),
                viewer.getLastReadMessageId()
        );
    }

    /**
     * 승인된 우선순위(BLOCKED → COUNTERPART_WITHDRAWN → COUNTERPART_RESTRICTED → MEMBER_RESTRICTED →
     * COUNTERPART_LEFT → OTHER_EXCHANGE_COMPLETED)로 사유를 하나만 고른다. 겹치는 사유가 없으면 null
     */
    private static ChatInputDisabledReason resolveInputDisabledReason(boolean blocked, boolean counterpartWithdrawn,
            boolean counterpartRestricted, boolean viewerRestricted, boolean counterpartLeft,
            boolean otherExchangeCompleted) {
        if (blocked) {
            return ChatInputDisabledReason.BLOCKED;
        }
        if (counterpartWithdrawn) {
            return ChatInputDisabledReason.COUNTERPART_WITHDRAWN;
        }
        if (counterpartRestricted) {
            return ChatInputDisabledReason.COUNTERPART_RESTRICTED;
        }
        if (viewerRestricted) {
            return ChatInputDisabledReason.MEMBER_RESTRICTED;
        }
        if (counterpartLeft) {
            return ChatInputDisabledReason.COUNTERPART_LEFT;
        }
        if (otherExchangeCompleted) {
            return ChatInputDisabledReason.OTHER_EXCHANGE_COMPLETED;
        }
        return null;
    }

    private static ChatRoomLeaveImpact leaveImpactOf(ChatAppointmentState appointmentState) {
        return switch (appointmentState) {
            case NONE -> ChatRoomLeaveImpact.NONE;
            case CONFIRMED -> ChatRoomLeaveImpact.UNILATERAL_CANCEL;
            case CONFIRMATION_OPENED -> ChatRoomLeaveImpact.COMPLETE_EXCHANGE;
        };
    }
}
