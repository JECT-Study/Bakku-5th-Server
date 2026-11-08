package com.ject.notification.api.controller;

import com.ject.common.web.response.ApiResponse;
import com.ject.notification.api.controller.api.NotificationApi;
import com.ject.notification.api.dto.enums.NotificationTargetType;
import com.ject.notification.api.dto.enums.NotificationType;
import com.ject.notification.api.dto.response.CursorPageResponse;
import com.ject.notification.api.dto.response.NotificationReadResponse;
import com.ject.notification.api.dto.response.NotificationResponse;
import com.ject.notification.api.dto.response.NotificationTargetResponse;
import com.ject.notification.api.dto.response.ReadAllResponse;
import com.ject.notification.api.dto.response.UnreadCountResponse;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.ject.notification.api.controller.NotificationStubData.NOW;

@RestController
public class NotificationController implements NotificationApi {

    @Override
    public ApiResponse<CursorPageResponse<NotificationResponse>> getNotifications(String cursor, Integer size) {
        NotificationTargetResponse chatRoom = new NotificationTargetResponse(NotificationTargetType.CHAT_ROOM, 5L, null);
        return ApiResponse.success(new CursorPageResponse<>(List.of(
                new NotificationResponse(9001L, NotificationType.APPOINTMENT_PROPOSED, "교환 약속 제안이 왔어요",
                        "하치와레러버님이 12/2(화) 15:00 홍대입구역에서 만나자고 했어요", chatRoom, false, NOW),
                new NotificationResponse(9000L, NotificationType.CHAT_MESSAGE, "하치와레러버",
                        "혹시 내일도 가능하세요?", chatRoom, true, NOW.minusMinutes(30))),
                "eyJpZCI6OTAwMH0", true));
    }

    @Override
    public ApiResponse<UnreadCountResponse> getUnreadCount() {
        return ApiResponse.success(new UnreadCountResponse(3));
    }

    @Override
    public ApiResponse<NotificationReadResponse> read(Long notificationId) {
        return ApiResponse.success(new NotificationReadResponse(notificationId, true));
    }

    @Override
    public ApiResponse<ReadAllResponse> readAll() {
        return ApiResponse.success(new ReadAllResponse(3));
    }
}
