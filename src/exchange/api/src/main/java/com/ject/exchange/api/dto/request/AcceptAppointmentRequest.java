// 출시 후 검토 (E-06-1, #117): 각자 약속 전 알림 설정. 이번 출시는 30분 고정이라 쓰지 않는다
// package com.ject.exchange.api.dto.request;
//
// import com.ject.exchange.api.dto.validation.TenMinuteStep;
// import com.ject.exchange.application.dto.command.AcceptAppointmentCommand;
// import io.swagger.v3.oas.annotations.media.Schema;
// import jakarta.validation.constraints.Max;
// import jakarta.validation.constraints.Min;
//
// @Schema(description = "약속 수락")
// public record AcceptAppointmentRequest(
//         @Schema(description = "수락자 본인의 약속 전 알림(분). null 끄기, 10분 단위 10~1440. 지금 바로 약속이면 무시", example = "30")
//         @Min(10) @Max(1440) @TenMinuteStep
//         Integer reminderOffsetMinutes
// ) {
//
//     public AcceptAppointmentCommand toCommand(Long appointmentId, Long accepterId) {
//         return new AcceptAppointmentCommand(appointmentId, accepterId, reminderOffsetMinutes);
//     }
// }
