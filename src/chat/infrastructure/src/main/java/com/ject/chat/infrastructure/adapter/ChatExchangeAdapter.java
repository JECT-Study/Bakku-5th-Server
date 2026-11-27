package com.ject.chat.infrastructure.adapter;

import com.ject.chat.application.dto.ChatAppointmentState;
import com.ject.chat.application.dto.ChatExchangeState;
import com.ject.chat.application.port.ChatExchangePort;
import com.ject.exchange.application.contract.ChatRoomAppointmentState;
import com.ject.exchange.application.contract.ChatRoomExchangeQuery;
import com.ject.exchange.application.contract.ChatRoomExchangeState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * exchange BC의 교환 · 약속 상태 계약을 chat 상세 조회에 필요한 형태로 조회한다
 */
@Component
@RequiredArgsConstructor
public class ChatExchangeAdapter implements ChatExchangePort {

    private final ChatRoomExchangeQuery chatRoomExchangeQuery;

    @Override
    public ChatExchangeState getExchangeState(Long exchangeId, Long viewerId) {
        ChatRoomExchangeState state = chatRoomExchangeQuery.getChatRoomState(exchangeId, viewerId);
        return new ChatExchangeState(state.otherExchangeCompleted(), toAppointmentState(state.appointmentState()));
    }

    private static ChatAppointmentState toAppointmentState(ChatRoomAppointmentState appointmentState) {
        return switch (appointmentState) {
            case NONE -> ChatAppointmentState.NONE;
            case CONFIRMED -> ChatAppointmentState.CONFIRMED;
            case CONFIRMATION_OPENED -> ChatAppointmentState.CONFIRMATION_OPENED;
        };
    }
}
