package com.ject.user.api.controller;

import com.ject.common.web.response.ApiResponse;
import com.ject.user.api.controller.api.DeviceApi;
import com.ject.user.api.dto.request.DeviceRequest;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DeviceController implements DeviceApi {

    @Override
    public ApiResponse<Void> registerDevice(DeviceRequest request) {
        return ApiResponse.success(null);
    }
}
