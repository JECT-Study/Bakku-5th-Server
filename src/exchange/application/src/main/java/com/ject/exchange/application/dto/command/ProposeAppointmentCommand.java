package com.ject.exchange.application.dto.command;

import com.ject.common.core.cqrs.command.Command;
import com.ject.exchange.domain.domain.appointment.AppointmentTimeType;
import com.ject.exchange.domain.domain.appointment.vo.Place;

import java.time.Instant;
import java.util.List;

public record ProposeAppointmentCommand(
        /**
         * 약속을 제안할 교환 ID
         */
        Long exchangeId,

        /**
         * 제안하는 회원 ID. 교환 참여자여야 한다
         */
        Long proposerId,

        /**
         * 시간 유형. NOW 지금 바로 / SCHEDULED 날짜 · 시간 지정
         */
        AppointmentTimeType timeType,

        /**
         * SCHEDULED일 때 약속 시각. NOW면 무시
         */
        Instant scheduledAt,

        /**
         * 약속 장소
         */
        Place place,

        /**
         * 교환할 물건 (이 교환의 첨부 아이템 ID). null이면 고를 수 있는 아이템이 1개일 때 자동 선택
         */
        List<Long> offeredItemIds

        // 출시 후 검토 (E-06-1, #117): 제안자의 약속 전 알림(분). 이번 출시는 30분 고정
        // Integer reminderOffsetMinutes
) implements Command<Long> {
}
