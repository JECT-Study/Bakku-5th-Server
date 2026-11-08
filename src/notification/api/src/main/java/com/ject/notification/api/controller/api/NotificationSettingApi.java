package com.ject.notification.api.controller.api;

import com.ject.common.web.response.ApiResponse;
import com.ject.notification.api.dto.request.NotificationSettingUpdateRequest;
import com.ject.notification.api.dto.response.NotificationSettingListResponse;
import com.ject.notification.api.dto.response.NotificationSettingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "NotificationSetting", description = "알림 설정 (2차)")
@RequestMapping("/api/v1/members/me/notification-settings")
public interface NotificationSettingApi {

    @Operation(summary = "NT-5 알림 설정 조회 (2차)",
            description = "설정 > 알림의 토글 6종입니다. 꺼도 앱 안 알림 목록에는 쌓이고 푸시만 보내지 않습니다. 1차 알림 설정은 마케팅 수신(MB-7)만 있습니다.")
    @GetMapping
    ApiResponse<NotificationSettingListResponse> getSettings();

    @Operation(summary = "NT-6 알림 설정 변경 (2차)",
            description = "누르면 바로 저장합니다. MARKETING은 MB-7과 같은 값입니다.")
    @PatchMapping
    ApiResponse<NotificationSettingResponse> updateSetting(@Valid @RequestBody NotificationSettingUpdateRequest request);
}
