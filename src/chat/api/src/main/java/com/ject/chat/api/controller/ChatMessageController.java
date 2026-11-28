package com.ject.chat.api.controller;

import com.ject.chat.api.controller.api.ChatMessageApi;
import com.ject.chat.api.dto.enums.MessageType;
import com.ject.chat.api.dto.request.SendMessageRequest;
import com.ject.chat.api.dto.response.CursorPageResponse;
import com.ject.chat.api.dto.response.MessageResponse;
import com.ject.chat.api.dto.response.TextMessageContent;
import com.ject.chat.api.exception.CursorAfterConflictException;
import com.ject.chat.application.dto.ChatMessagePage;
import com.ject.chat.application.dto.query.GetChatMessagesQuery;
import com.ject.common.core.cqrs.query.QueryBus;
import com.ject.common.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.ject.chat.api.controller.ChatStubData.LATEST_MESSAGE_ID;
import static com.ject.chat.api.controller.ChatStubData.LATEST_MESSAGE_SENT_AT;
import static com.ject.chat.api.controller.ChatStubData.LATEST_MESSAGE_TEXT;
import static com.ject.chat.api.controller.ChatStubData.MY_USER_ID;

@RestController
@RequiredArgsConstructor
public class ChatMessageController implements ChatMessageApi {

    private final QueryBus queryBus;

    @Override
    public ApiResponse<CursorPageResponse<MessageResponse>> getMessages(Long memberId, Long chatRoomId, String cursor,
                                                                        Integer size, Long after) {
        if (cursor != null && after != null) {
            throw new CursorAfterConflictException();
        }
        GetChatMessagesQuery query = GetChatMessagesQuery.builder()
                .chatRoomId(chatRoomId)
                .viewerId(memberId)
                .cursorMessageId(ChatMessageCursor.decode(cursor))
                .size(size)
                .afterMessageId(after)
                .build();
        ChatMessagePage page = queryBus.send(query);
        List<MessageResponse> messages = page.content().stream().map(MessageResponse::from).toList();
        return ApiResponse.success(new CursorPageResponse<>(messages,
                ChatMessageCursor.encode(page.nextCursorMessageId()), page.hasNext()));
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
