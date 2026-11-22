package com.ject.exchange.application.dto.dto;

import com.ject.exchange.domain.domain.appointment.AppointmentStatus;
import com.ject.exchange.domain.domain.appointment.ConfirmationAnswer;

import java.time.Instant;

/**
 * 조회자 기준 교환 확인 정보
 * @param askedAt 교환 확인이 열리는 시각. 열리기 전이면 null
 * @param result 결과(COMPLETED · NOT_MET). 결과 전이면 null
 * @param autoCompleted 3일 무응답 자동 완료 여부
 * @param myAnswer 조회자의 응답. 응답 전이면 null
 * @param resolvedAt 결과 시각. 결과 전이면 null
 */
public record ConfirmationSummary(Instant askedAt, AppointmentStatus result, boolean autoCompleted,
                                  ConfirmationAnswer myAnswer, Instant resolvedAt) {
}
