package com.ject.user.api.controller;

import com.ject.common.web.response.ApiResponse;
import com.ject.user.api.controller.api.AppVersionApi;
import com.ject.user.api.dto.enums.DevicePlatform;
import com.ject.user.api.dto.response.AppVersionResponse;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AppVersionController implements AppVersionApi {

    @Override
    public ApiResponse<AppVersionResponse> getVersion(DevicePlatform platform) {
        String storeUrl = platform == DevicePlatform.IOS
                ? "https://apps.apple.com/app/id0000000000"
                : "https://play.google.com/store/apps/details?id=com.bakku.app";
        return ApiResponse.success(new AppVersionResponse("1.0.0", storeUrl));
    }
}
