package com.ject.user.api.controller;

import com.ject.common.web.response.ApiResponse;
import com.ject.user.api.controller.api.DeviceApi;
import com.ject.user.api.dto.request.DeviceRequest;
import com.ject.user.application.dto.RegisterDeviceCommand;
import com.ject.user.application.service.DeviceService;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DeviceController implements DeviceApi {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @Override
    public ApiResponse<Void> registerDevice(Long memberId, DeviceRequest request) {
        deviceService.register(memberId, new RegisterDeviceCommand(request.deviceId(), request.pushToken(),
                request.platform().name(), request.appVersion()));
        return ApiResponse.success(null);
    }
}
