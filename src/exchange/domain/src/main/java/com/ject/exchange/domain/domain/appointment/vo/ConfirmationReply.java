package com.ject.exchange.domain.domain.appointment.vo;

import com.ject.exchange.domain.domain.appointment.ConfirmationAnswer;
import com.ject.exchange.domain.domain.appointment.NotMetReason;

import java.time.Instant;

/**
 * 한 참여자의 교환 확인 응답 (1인 1회). 교환했어요면 이유 · 직접 입력은 null
 */
public record ConfirmationReply(Long userId, ConfirmationAnswer answer, NotMetReason notMetReason,
                                String notMetDetail, Instant answeredAt) {
}
