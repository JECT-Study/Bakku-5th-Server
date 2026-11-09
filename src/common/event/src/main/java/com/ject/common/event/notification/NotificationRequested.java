package com.ject.common.event.notification;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 다른 파트가 알림을 요청할 때 발행한다. 문구 생성 · 저장 · 푸시 · 예약은 알림이 담당한다 (결정 B2).
 *
 * @param senderId    보낸 회원 id, 시스템 알림이면 null
 * @param target      알림을 눌렀을 때 이동할 대상, 없으면 null
 * @param params      문구 생성용 값, null이면 빈 값으로 처리
 * @param scheduledAt 예약 발송 시각, 바로 보내면 null
 * @param scheduleKey 예약 취소 · 재예약용 키, 예약이 아니면 null
 */
public record NotificationRequested(
        NotificationType type,
        List<Long> receiverIds,
        Long senderId,
        Target target,
        Map<String, String> params,
        Instant scheduledAt,
        String scheduleKey
) {

    public NotificationRequested {
        Objects.requireNonNull(type);
        receiverIds = List.copyOf(receiverIds);
        params = params == null ? Map.of() : Map.copyOf(params);
    }

    public record Target(NotificationTargetType type, Long id) {
    }
}
