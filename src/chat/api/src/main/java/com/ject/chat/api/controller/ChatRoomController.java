package com.ject.chat.api.controller;

import com.ject.chat.api.controller.api.ChatRoomApi;
import com.ject.chat.api.dto.enums.LeaveImpact;
import com.ject.chat.api.dto.enums.PostStatus;
import com.ject.chat.api.dto.enums.RoomBadge;
import com.ject.chat.api.dto.enums.RoomStatus;
import com.ject.chat.api.dto.request.ReadMessagesRequest;
import com.ject.chat.api.dto.request.UpdateRoomNotificationRequest;
import com.ject.chat.api.dto.response.ChatPostResponse;
import com.ject.chat.api.dto.response.ChatRoomResponse;
import com.ject.chat.api.dto.response.ChatRoomSummaryResponse;
import com.ject.chat.api.dto.response.CounterpartResponse;
import com.ject.chat.api.dto.response.CounterpartSummaryResponse;
import com.ject.chat.api.dto.response.CursorPageResponse;
import com.ject.chat.api.dto.response.LastMessageResponse;
import com.ject.chat.api.dto.response.LeaveChatRoomResponse;
import com.ject.chat.api.dto.response.ReadMessagesResponse;
import com.ject.common.response.ApiResponse;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.ject.chat.api.controller.ChatStubData.CHAT_ROOM_ID;
import static com.ject.chat.api.controller.ChatStubData.COUNTERPART_ID;
import static com.ject.chat.api.controller.ChatStubData.COUNTERPART_NICKNAME;
import static com.ject.chat.api.controller.ChatStubData.EXCHANGE_ID;
import static com.ject.chat.api.controller.ChatStubData.at;

@RestController
public class ChatRoomController implements ChatRoomApi {

    @Override
    public ApiResponse<CursorPageResponse<ChatRoomSummaryResponse>> getChatRooms(String cursor, Integer size) {
        List<ChatRoomSummaryResponse> rooms = List.of(
                new ChatRoomSummaryResponse(
                        CHAT_ROOM_ID, EXCHANGE_ID,
                        new CounterpartSummaryResponse(COUNTERPART_ID, COUNTERPART_NICKNAME, false),
                        new ChatPostResponse(10L, "https://{cdn}/post/10/1.jpg", PostStatus.RESERVED),
                        RoomBadge.RESERVED,
                        new LastMessageResponse(120L, "내일 3시 괜찮으세요?", at(12, 14, 30)),
                        3, true),
                new ChatRoomSummaryResponse(
                        6L, 2L,
                        new CounterpartSummaryResponse(303L, "먼작귀수집가", false),
                        new ChatPostResponse(14L, "https://{cdn}/post/14/1.jpg", PostStatus.AVAILABLE),
                        null,
                        new LastMessageResponse(98L, "사진", at(12, 11, 5)),
                        0, true),
                new ChatRoomSummaryResponse(
                        8L, 4L,
                        new CounterpartSummaryResponse(404L, "모몽가조아", true),
                        new ChatPostResponse(17L, null, PostStatus.COMPLETED),
                        RoomBadge.COMPLETED,
                        new LastMessageResponse(76L, "교환 성공!", at(10, 19, 20)),
                        0, false));
        return ApiResponse.success(new CursorPageResponse<>(rooms, "eyJ0IjoiMjAyNi0xMC0xMFQxOToyMDowMCJ9", true));
    }

    @Override
    public ApiResponse<ChatRoomResponse> getChatRoom(Long chatRoomId) {
        return ApiResponse.success(chatRoom(chatRoomId, true));
    }

    @Override
    public ApiResponse<ReadMessagesResponse> readMessages(Long chatRoomId, ReadMessagesRequest request) {
        return ApiResponse.success(new ReadMessagesResponse(chatRoomId, request.lastReadMessageId(), 0));
    }

    @Override
    public ApiResponse<ChatRoomResponse> updateNotification(Long chatRoomId, UpdateRoomNotificationRequest request) {
        return ApiResponse.success(chatRoom(chatRoomId, request.enabled()));
    }

    @Override
    public ApiResponse<LeaveChatRoomResponse> leaveChatRoom(Long chatRoomId) {
        return ApiResponse.success(new LeaveChatRoomResponse(chatRoomId, true, false));
    }

    private static ChatRoomResponse chatRoom(Long chatRoomId, boolean notificationEnabled) {
        return new ChatRoomResponse(
                chatRoomId, EXCHANGE_ID, RoomStatus.OPEN, null,
                new CounterpartResponse(COUNTERPART_ID, COUNTERPART_NICKNAME, 5, false),
                notificationEnabled, LeaveImpact.NONE, 117L);
    }
}
