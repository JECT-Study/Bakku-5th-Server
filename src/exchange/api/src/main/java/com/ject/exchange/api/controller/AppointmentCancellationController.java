package com.ject.exchange.api.controller;

import com.ject.common.response.ApiResponse;
import com.ject.exchange.api.controller.api.AppointmentCancellationApi;
import com.ject.exchange.api.dto.enums.CancellationStatus;
import com.ject.exchange.api.dto.enums.CancellationType;
import com.ject.exchange.api.dto.response.CancellationResponse;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

import static com.ject.exchange.api.controller.ExchangeStubData.APPOINTMENT_ID;
import static com.ject.exchange.api.controller.ExchangeStubData.CANCELLATION_ID;
import static com.ject.exchange.api.controller.ExchangeStubData.REQUESTER_ID;
import static com.ject.exchange.api.controller.ExchangeStubData.at;

@RestController
public class AppointmentCancellationController implements AppointmentCancellationApi {

    @Override
    public ApiResponse<CancellationResponse> requestCancellation(Long appointmentId) {
        return ApiResponse.created(cancellation(CANCELLATION_ID, appointmentId, CancellationStatus.REQUESTED, null));
    }

    @Override
    public ApiResponse<CancellationResponse> agreeCancellation(Long cancellationId) {
        return ApiResponse.success(cancellation(cancellationId, APPOINTMENT_ID, CancellationStatus.AGREED, at(27, 13, 10)));
    }

    @Override
    public ApiResponse<CancellationResponse> rejectCancellation(Long cancellationId) {
        return ApiResponse.success(cancellation(cancellationId, APPOINTMENT_ID, CancellationStatus.REJECTED, null));
    }

    @Override
    public ApiResponse<CancellationResponse> forceCancellation(Long cancellationId) {
        return ApiResponse.success(cancellation(cancellationId, APPOINTMENT_ID, CancellationStatus.UNILATERAL, at(27, 13, 40)));
    }

    @Override
    public ApiResponse<CancellationResponse> withdrawCancellation(Long cancellationId) {
        return ApiResponse.success(cancellation(cancellationId, APPOINTMENT_ID, CancellationStatus.WITHDRAWN, at(27, 13, 15)));
    }

    private static CancellationResponse cancellation(Long id, Long appointmentId, CancellationStatus status,
                                                     OffsetDateTime resolvedAt) {
        return new CancellationResponse(
                id, appointmentId, REQUESTER_ID, CancellationType.REQUEST, status, false,
                at(27, 13, 30), at(27, 13, 0), resolvedAt);
    }
}
