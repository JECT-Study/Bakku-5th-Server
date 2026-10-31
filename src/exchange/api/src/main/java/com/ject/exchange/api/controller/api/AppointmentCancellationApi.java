package com.ject.exchange.api.controller.api;

import com.ject.common.response.ApiResponse;
import com.ject.exchange.api.dto.response.CancellationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "Appointment Cancellation", description = "약속 취소 요청 · 동의 · 거절 · 일방 취소 · 철회")
@RequestMapping("/api/v1")
public interface AppointmentCancellationApi {

    @Operation(summary = "C-1 취소 요청", description = "확정된 약속의 취소를 상대에게 요청합니다. 진행 중인 취소 요청은 약속당 1개입니다.")
    @PostMapping("/appointments/{appointmentId}/cancellations")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<CancellationResponse> requestCancellation(@PathVariable("appointmentId") Long appointmentId);

    @Operation(summary = "C-2 취소 동의", description = "합의 취소입니다.")
    @PostMapping("/appointment-cancellations/{cancellationId}/agree")
    ApiResponse<CancellationResponse> agreeCancellation(@PathVariable("cancellationId") Long cancellationId);

    @Operation(summary = "C-3 취소 거절", description = "약속은 유지되고, 요청자는 일방 취소(C-4)를 고를 수 있습니다.")
    @PostMapping("/appointment-cancellations/{cancellationId}/reject")
    ApiResponse<CancellationResponse> rejectCancellation(@PathVariable("cancellationId") Long cancellationId);

    @Operation(summary = "C-4 일방 취소", description = "상대가 거절했거나(REJECTED), forceAvailableAt이 지났을 때만 가능합니다.")
    @PostMapping("/appointment-cancellations/{cancellationId}/force")
    ApiResponse<CancellationResponse> forceCancellation(@PathVariable("cancellationId") Long cancellationId);

    @Operation(summary = "C-5 취소 요청 철회", description = "요청자가 취소 요청을 거둡니다. 약속은 확정 상태로 유지됩니다.")
    @PostMapping("/appointment-cancellations/{cancellationId}/withdraw")
    ApiResponse<CancellationResponse> withdrawCancellation(@PathVariable("cancellationId") Long cancellationId);
}
