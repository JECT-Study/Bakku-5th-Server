package com.ject.notification.api.controller;

import com.ject.common.core.time.ServiceTime;
import com.ject.common.web.response.ApiResponse;
import com.ject.notification.api.controller.api.NotificationApi;
import com.ject.notification.api.dto.response.CursorPageResponse;
import com.ject.notification.api.dto.response.NotificationReadResponse;
import com.ject.notification.api.dto.response.NotificationResponse;
import com.ject.notification.api.dto.response.NotificationTargetResponse;
import com.ject.notification.api.dto.response.ReadAllResponse;
import com.ject.notification.api.dto.response.UnreadCountResponse;
import com.ject.notification.application.dto.NotificationPage;
import com.ject.notification.application.dto.NotificationPage.NotificationItem;
import com.ject.notification.application.service.NotificationQueryService;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationController implements NotificationApi {

    private final NotificationQueryService notificationQueryService;
    private final ServiceTime serviceTime;

    public NotificationController(NotificationQueryService notificationQueryService, ServiceTime serviceTime) {
        this.notificationQueryService = notificationQueryService;
        this.serviceTime = serviceTime;
    }

    @Override
    public ApiResponse<CursorPageResponse<NotificationResponse>> getNotifications(Long memberId, String cursor,
                                                                                 Integer size) {
        NotificationPage page = notificationQueryService.getNotifications(memberId,
                NotificationCursor.decode(cursor), size);
        return ApiResponse.success(new CursorPageResponse<>(page.content().stream().map(this::toResponse).toList(),
                NotificationCursor.encode(page.nextCursorId()), page.hasNext()));
    }

    @Override
    public ApiResponse<UnreadCountResponse> getUnreadCount(Long memberId) {
        return ApiResponse.success(new UnreadCountResponse(notificationQueryService.getUnreadCount(memberId)));
    }

    @Override
    public ApiResponse<NotificationReadResponse> read(Long notificationId) {
        return ApiResponse.success(new NotificationReadResponse(notificationId, true));
    }

    @Override
    public ApiResponse<ReadAllResponse> readAll() {
        return ApiResponse.success(new ReadAllResponse(3));
    }

    private NotificationResponse toResponse(NotificationItem item) {
        NotificationTargetResponse target = item.targetType() == null ? null
                : new NotificationTargetResponse(item.targetType(), item.targetId(), null);
        return new NotificationResponse(item.notificationId(), item.type(), item.title(), item.body(), target,
                item.read(), serviceTime.toOffset(item.createdAt()));
    }
}
