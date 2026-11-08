package com.ject.notification.api.controller.api;

import com.ject.common.web.response.ApiResponse;
import com.ject.notification.api.dto.response.CursorPageResponse;
import com.ject.notification.api.dto.response.NotificationReadResponse;
import com.ject.notification.api.dto.response.NotificationResponse;
import com.ject.notification.api.dto.response.ReadAllResponse;
import com.ject.notification.api.dto.response.UnreadCountResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Notification", description = "알림 목록 · 안 읽은 수 · 읽음 처리")
@RequestMapping("/api/v1/notifications")
public interface NotificationApi {

    @Operation(summary = "NT-1 알림 목록",
            description = "최신순, 30일 지난 알림은 보이지 않습니다. 알림은 이벤트마다 1건씩 저장합니다 (채팅도 메시지마다). 문구(title · body)는 서버가 만듭니다. "
                    + "누르면 NT-3으로 읽음 처리 후 target으로 이동합니다. 1차 알림은 9종이고 모두 CHAT_ROOM으로 이동합니다.")
    @GetMapping
    ApiResponse<CursorPageResponse<NotificationResponse>> getNotifications(
            @Parameter(description = "이전 응답의 nextCursor. 첫 요청은 생략")
            @RequestParam(value = "cursor", required = false) String cursor,
            @Parameter(description = "1~50, 기본 20")
            @RequestParam(value = "size", defaultValue = "20") @Min(1) @Max(50) Integer size);

    @Operation(summary = "NT-2 안 읽은 알림 수",
            description = "상단 알림 아이콘 배지에 씁니다. 앱 실행 · 앱이 앞으로 돌아올 때 · 푸시를 받았을 때 FE가 다시 부릅니다.")
    @GetMapping("/unread-count")
    ApiResponse<UnreadCountResponse> getUnreadCount();

    @Operation(summary = "NT-3 알림 읽음",
            description = "알림을 누르면 읽음 처리한 뒤 해당 화면으로 갑니다. 이미 읽은 알림이어도 성공으로 처리합니다. "
                    + "에러: NOTIFICATION_001 알림 없음 (남의 알림 · 30일 지나 삭제됨 포함) → 목록 새로고침")
    @PostMapping("/{notificationId}/read")
    ApiResponse<NotificationReadResponse> read(
            @Parameter(description = "알림 ID") @PathVariable("notificationId") Long notificationId);

    @Operation(summary = "NT-4 모두 읽음", description = "안 읽은 알림을 전부 읽음 처리합니다.")
    @PostMapping("/read-all")
    ApiResponse<ReadAllResponse> readAll();
}
