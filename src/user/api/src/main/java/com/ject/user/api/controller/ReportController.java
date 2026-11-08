package com.ject.user.api.controller;

import com.ject.common.web.response.ApiResponse;
import com.ject.user.api.controller.api.ReportApi;
import com.ject.user.api.dto.request.ReportRequest;
import com.ject.user.api.dto.response.ReportResponse;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReportController implements ReportApi {

    @Override
    public ApiResponse<ReportResponse> report(ReportRequest request) {
        return ApiResponse.created(new ReportResponse(31L));
    }
}
