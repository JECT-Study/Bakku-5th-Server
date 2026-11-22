package com.ject.user.api.controller;

import com.ject.common.web.response.ApiResponse;
import com.ject.user.api.controller.api.ReportApi;
import com.ject.user.api.dto.request.ReportRequest;
import com.ject.user.api.dto.response.ReportResponse;
import com.ject.user.application.dto.ReportCommand;
import com.ject.user.application.service.ReportService;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReportController implements ReportApi {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @Override
    public ApiResponse<ReportResponse> report(Long memberId, ReportRequest request) {
        Long reportId = reportService.report(new ReportCommand(memberId, request.targetType().name(),
                request.targetId(), request.reason().name(), request.detail()));
        return ApiResponse.created(new ReportResponse(reportId));
    }
}
