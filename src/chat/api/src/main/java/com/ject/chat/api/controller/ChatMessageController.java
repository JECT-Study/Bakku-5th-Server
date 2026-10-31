package com.ject.chat.api.controller;

import com.ject.chat.api.controller.api.ChatMessageApi;
import com.ject.chat.api.dto.enums.CardType;
import com.ject.chat.api.dto.enums.MessageType;
import com.ject.chat.api.dto.enums.SystemLinkType;
import com.ject.chat.api.dto.enums.SystemMessageCode;
import com.ject.chat.api.dto.request.SendMessageRequest;
import com.ject.chat.api.dto.response.CardMessageContent;
import com.ject.chat.api.dto.response.CursorPageResponse;
import com.ject.chat.api.dto.response.MessageResponse;
import com.ject.chat.api.dto.response.SystemMessageContent;
import com.ject.chat.api.dto.response.SystemMessageLink;
import com.ject.chat.api.dto.response.TextMessageContent;
import com.ject.common.response.ApiResponse;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.ject.chat.api.controller.ChatStubData.APPOINTMENT_ID;
import static com.ject.chat.api.controller.ChatStubData.CLIENT_MESSAGE_ID;
import static com.ject.chat.api.controller.ChatStubData.LATEST_MESSAGE_ID;
import static com.ject.chat.api.controller.ChatStubData.LATEST_MESSAGE_SENT_AT;
import static com.ject.chat.api.controller.ChatStubData.LATEST_MESSAGE_TEXT;
import static com.ject.chat.api.controller.ChatStubData.MY_USER_ID;
import static com.ject.chat.api.controller.ChatStubData.at;

@RestController
public class ChatMessageController implements ChatMessageApi {

    @Override
    public ApiResponse<CursorPageResponse<MessageResponse>> getMessages(Long chatRoomId, String cursor, Integer size,
                                                                        Long after) {
        List<MessageResponse> messages = List.of(
                textMessage(chatRoomId, CLIENT_MESSAGE_ID),
                new MessageResponse(
                        120L, chatRoomId, null, MessageType.CARD,
                        new CardMessageContent(CardType.APPOINTMENT_PROPOSAL, APPOINTMENT_ID),
                        null, at(12, 14, 29)),
                new MessageResponse(
                        119L, chatRoomId, null, MessageType.SYSTEM,
                        new SystemMessageContent(SystemMessageCode.ATTACHED_POST_UPDATED, "상대가 첨부한 글을 수정했어요",
                                new SystemMessageLink(SystemLinkType.POST, 21L)),
                        null, at(12, 14, 10)));
        return ApiResponse.success(new CursorPageResponse<>(messages, "eyJpZCI6OTB9", true));
    }

    @Override
    public ApiResponse<MessageResponse> sendMessage(Long chatRoomId, SendMessageRequest request) {
        return ApiResponse.created(textMessage(chatRoomId, request.clientMessageId()));
    }

    private static MessageResponse textMessage(Long chatRoomId, String clientMessageId) {
        return new MessageResponse(
                LATEST_MESSAGE_ID, chatRoomId, MY_USER_ID, MessageType.TEXT,
                new TextMessageContent(LATEST_MESSAGE_TEXT, false),
                clientMessageId, LATEST_MESSAGE_SENT_AT);
    }
}
