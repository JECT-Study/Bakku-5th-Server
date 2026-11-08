package com.ject.notification.api.controller;

import com.ject.common.web.response.ApiResponse;
import com.ject.notification.api.controller.api.NotificationSettingApi;
import com.ject.notification.api.dto.enums.NotificationSettingType;
import com.ject.notification.api.dto.request.NotificationSettingUpdateRequest;
import com.ject.notification.api.dto.response.NotificationSettingListResponse;
import com.ject.notification.api.dto.response.NotificationSettingResponse;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

import static com.ject.notification.api.controller.NotificationStubData.NOW;

@RestController
public class NotificationSettingController implements NotificationSettingApi {

    @Override
    public ApiResponse<NotificationSettingListResponse> getSettings() {
        return ApiResponse.success(new NotificationSettingListResponse(Arrays.stream(NotificationSettingType.values())
                .map(type -> type == NotificationSettingType.MARKETING
                        ? new NotificationSettingResponse(type, false, NOW.minusDays(4))
                        : new NotificationSettingResponse(type, true, null))
                .toList()));
    }

    @Override
    public ApiResponse<NotificationSettingResponse> updateSetting(NotificationSettingUpdateRequest request) {
        return ApiResponse.success(new NotificationSettingResponse(request.type(), request.enabled(), NOW));
    }
}
