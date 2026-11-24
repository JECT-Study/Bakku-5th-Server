// 출시 후 검토 (E-06-1, #117): 각자 약속 전 알림 설정. 이번 출시는 30분 고정이라 쓰지 않는다
// package com.ject.exchange.api.dto.request;
//
// import com.ject.exchange.api.dto.validation.TenMinuteStep;
// import com.ject.exchange.application.dto.command.UpdateReminderCommand;
// import io.swagger.v3.oas.annotations.media.Schema;
// import jakarta.validation.constraints.Max;
// import jakarta.validation.constraints.Min;
//
// @Schema(description = "내 약속 전 알림 변경")
// public record UpdateReminderRequest(
//         @Schema(description = "약속 전 알림(분). null 끄기, 10분 단위 10~1440 (최대 하루 전)", example = "60")
//         @Min(10) @Max(1440) @TenMinuteStep
//         Integer reminderOffsetMinutes
// ) {
//
//     public UpdateReminderCommand toCommand(Long appointmentId, Long memberId) {
//         return new UpdateReminderCommand(appointmentId, memberId, reminderOffsetMinutes);
//     }
// }
