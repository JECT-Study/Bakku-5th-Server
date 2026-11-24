// 출시 후 검토 (E-06-1, #117): A-7 내 약속 전 알림 변경. 이번 출시는 30분 고정이라 쓰지 않는다
// package com.ject.exchange.application.dto.command;
//
// import com.ject.common.core.cqrs.command.Command;
//
// public record UpdateReminderCommand(
//         /**
//          * 알림을 바꿀 약속 ID
//          */
//         Long appointmentId,
//
//         /**
//          * 바꾸는 회원 ID. 약속 참여자여야 한다
//          */
//         Long memberId,
//
//         /**
//          * 내 약속 전 알림(분). null이면 끄기
//          */
//         Integer reminderOffsetMinutes
// ) implements Command<Long> {
// }
