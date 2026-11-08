package com.ject.notification.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "알림 설정 (2차)")
public record NotificationSettingListResponse(
        @Schema(description = "토글 목록")
        List<NotificationSettingResponse> settings
) {
}
