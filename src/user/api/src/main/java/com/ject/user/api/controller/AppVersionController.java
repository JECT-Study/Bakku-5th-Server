package com.ject.user.api.controller;

import com.ject.common.web.response.ApiResponse;
import com.ject.user.api.config.AppVersionProperties;
import com.ject.user.api.controller.api.AppVersionApi;
import com.ject.user.api.dto.enums.DevicePlatform;
import com.ject.user.api.dto.response.AppVersionResponse;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AppVersionController implements AppVersionApi {

    private final AppVersionProperties appVersionProperties;

    public AppVersionController(AppVersionProperties appVersionProperties) {
        this.appVersionProperties = appVersionProperties;
    }

    @Override
    public ApiResponse<AppVersionResponse> getVersion(DevicePlatform platform) {
        AppVersionProperties.Platform version = switch (platform) {
            case IOS -> appVersionProperties.ios();
            case ANDROID -> appVersionProperties.android();
        };
        return ApiResponse.success(new AppVersionResponse(version.minimumVersion(), version.storeUrl()));
    }
}
