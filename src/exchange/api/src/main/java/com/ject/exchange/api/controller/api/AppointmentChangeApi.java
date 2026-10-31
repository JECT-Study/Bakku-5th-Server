package com.ject.exchange.api.controller.api;

import com.ject.common.response.ApiResponse;
import com.ject.exchange.api.dto.request.ProposeAppointmentChangeRequest;
import com.ject.exchange.api.dto.response.AppointmentChangeResponse;
import com.ject.exchange.api.dto.response.AppointmentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "Appointment Change", description = "확정 약속 변경 제안 · 수락 · 거절")
@RequestMapping("/api/v1")
public interface AppointmentChangeApi {

    @Operation(summary = "M-1 확정 약속 변경 제안", description = "채팅에 \"약속 변경을 제안해요\" 카드가 생깁니다. 대기 중인 변경 제안은 약속당 1개입니다.")
    @PostMapping("/appointments/{appointmentId}/changes")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<AppointmentChangeResponse> proposeAppointmentChange(@PathVariable("appointmentId") Long appointmentId,
                                                                    @Valid @RequestBody ProposeAppointmentChangeRequest request);

    @Operation(summary = "M-2 약속 변경 수락", description = "약속을 새 값으로 바꿉니다.")
    @PostMapping("/appointment-changes/{changeId}/accept")
    ApiResponse<AppointmentResponse> acceptAppointmentChange(@PathVariable("changeId") Long changeId);

    @Operation(summary = "M-3 약속 변경 거절", description = "기존 약속이 그대로 유지됩니다.")
    @PostMapping("/appointment-changes/{changeId}/reject")
    ApiResponse<AppointmentChangeResponse> rejectAppointmentChange(@PathVariable("changeId") Long changeId);
}
