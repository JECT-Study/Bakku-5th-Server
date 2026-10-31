package com.ject.exchange.api.controller.api;

import com.ject.common.response.ApiResponse;
import com.ject.exchange.api.dto.request.AcceptAppointmentRequest;
import com.ject.exchange.api.dto.request.ConfirmationRequest;
import com.ject.exchange.api.dto.request.ProposeAppointmentRequest;
import com.ject.exchange.api.dto.request.UpdateReminderRequest;
import com.ject.exchange.api.dto.response.AppointmentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Tag(name = "Appointment", description = "약속 제안 · 수락 · 거절 · 철회 · 알림 변경 · 교환 확인")
@RequestMapping("/api/v1")
public interface AppointmentApi {

    @Operation(summary = "A-1 약속 제안", description = "채팅방에 \"교환을 제안해요\" 카드가 생기고 상대에게 알림이 갑니다. 한 채팅방에 진행 중인 제안은 1개입니다.")
    @PostMapping("/exchanges/{exchangeId}/appointments")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<AppointmentResponse> proposeAppointment(@PathVariable("exchangeId") Long exchangeId,
                                                        @Valid @RequestBody ProposeAppointmentRequest request);

    @Operation(summary = "A-2 약속 상세", description = "약속 · 변경 제안 · 취소 요청 · 교환 확인 카드의 버튼 상태와 약속 상세 화면에 사용합니다.")
    @GetMapping("/appointments/{appointmentId}")
    ApiResponse<AppointmentResponse> getAppointment(@PathVariable("appointmentId") Long appointmentId);

    @Operation(summary = "A-3 현재 유효한 약속", description = "PROPOSED 또는 CONFIRMED 약속을 반환합니다. 유효한 약속이 없으면 data가 null입니다 (404 아님).")
    @GetMapping("/exchanges/{exchangeId}/appointments/current")
    ApiResponse<AppointmentResponse> getCurrentAppointment(@PathVariable("exchangeId") Long exchangeId);

    @Operation(summary = "A-4 약속 수락", description = "제안받은 사람이 약속을 수락합니다. 약속이 확정됩니다. 본문은 생략할 수 있습니다.")
    @PostMapping("/appointments/{appointmentId}/accept")
    ApiResponse<AppointmentResponse> acceptAppointment(@PathVariable("appointmentId") Long appointmentId,
                                                       @Valid @RequestBody(required = false) AcceptAppointmentRequest request);

    @Operation(summary = "A-5 약속 제안 거절", description = "제안을 무효로 하고 제안자에게 알림을 보냅니다.")
    @PostMapping("/appointments/{appointmentId}/reject")
    ApiResponse<AppointmentResponse> rejectAppointment(@PathVariable("appointmentId") Long appointmentId);

    @Operation(summary = "A-6 제안 철회", description = "제안자가 자기 제안을 취소합니다. 새로 제안하려면 진행 중인 제안을 먼저 철회해야 합니다.")
    @PostMapping("/appointments/{appointmentId}/withdraw")
    ApiResponse<AppointmentResponse> withdrawAppointment(@PathVariable("appointmentId") Long appointmentId);

    @Operation(summary = "A-7 내 약속 전 알림 변경", description = "상대의 설정에는 영향을 주지 않습니다. 알림 시각이 이미 지났으면 저장은 하되 발송하지 않습니다.")
    @PutMapping("/appointments/{appointmentId}/reminder")
    ApiResponse<AppointmentResponse> updateReminder(@PathVariable("appointmentId") Long appointmentId,
                                                    @Valid @RequestBody UpdateReminderRequest request);

    @Operation(summary = "F-1 교환 확인 응답", description = "한 명만 답해도 처리되고, 응답은 1인 1회이며 바꿀 수 없습니다.")
    @PostMapping("/appointments/{appointmentId}/confirmation")
    ApiResponse<AppointmentResponse> respondConfirmation(@PathVariable("appointmentId") Long appointmentId,
                                                         @Valid @RequestBody ConfirmationRequest request);
}
