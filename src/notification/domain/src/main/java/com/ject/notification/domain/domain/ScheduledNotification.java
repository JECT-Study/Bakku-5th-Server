package com.ject.notification.domain.domain;

import com.ject.common.event.notification.NotificationRequested;
import com.ject.common.event.notification.NotificationTargetType;
import com.ject.common.event.notification.NotificationType;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 정한 시각에 보낼 알림 (결정 C4). 받는 사람마다 한 건이고, 같은 예약 키 · 받는 사람이면 새 요청으로 바뀐다.
 * 양쪽 알림은 받는 사람마다 따로 발행되므로 키 하나에 여러 사람이 걸릴 수 있고, 취소는 키 단위로 한다.
 */
public class ScheduledNotification {

    private final Long id;
    private final String scheduleKey;
    private final Long receiverId;
    private final NotificationType type;
    private final Long senderId;
    private final NotificationTargetType targetType;
    private final Long targetId;
    private final Map<String, String> params;
    private final Instant fireAt;

    private ScheduledNotification(Long id, String scheduleKey, Long receiverId, NotificationType type,
                                  Long senderId, NotificationTargetType targetType, Long targetId,
                                  Map<String, String> params, Instant fireAt) {
        this.id = id;
        this.scheduleKey = scheduleKey;
        this.receiverId = Objects.requireNonNull(receiverId);
        this.type = Objects.requireNonNull(type);
        this.senderId = senderId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.params = params == null ? Map.of() : Map.copyOf(params);
        this.fireAt = Objects.requireNonNull(fireAt);
    }

    /**
     * 예약 요청을 받는 사람별로 나눈다. 같은 사람이 여러 번 있으면 한 건만 만든다.
     */
    public static List<ScheduledNotification> from(NotificationRequested request) {
        Objects.requireNonNull(request.scheduledAt(), "예약 시각이 없는 요청");
        NotificationTargetType targetType = request.target() == null ? null : request.target().type();
        Long targetId = request.target() == null ? null : request.target().id();
        return request.receiverIds().stream()
                .distinct()
                .map(receiverId -> new ScheduledNotification(null, request.scheduleKey(), receiverId,
                        request.type(), request.senderId(), targetType, targetId, request.params(),
                        request.scheduledAt()))
                .toList();
    }

    public static ScheduledNotification restore(Long id, String scheduleKey, Long receiverId, NotificationType type,
                                                Long senderId, NotificationTargetType targetType, Long targetId,
                                                Map<String, String> params, Instant fireAt) {
        return new ScheduledNotification(id, scheduleKey, receiverId, type, senderId, targetType, targetId, params,
                fireAt);
    }

    /**
     * 보낼 때가 되면 바로 보내는 요청으로 바꿔 일반 알림과 같은 길로 보낸다.
     */
    public NotificationRequested toRequest() {
        NotificationRequested.Target target = targetType == null ? null
                : new NotificationRequested.Target(targetType, targetId);
        return new NotificationRequested(type, List.of(receiverId), senderId, target, params, null, null);
    }

    /** 저장 전이면 null */
    public Long getId() {
        return id;
    }

    /** 키 없이 예약하면 null (바꾸거나 취소할 수 없음) */
    public String getScheduleKey() {
        return scheduleKey;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public NotificationType getType() {
        return type;
    }

    public Long getSenderId() {
        return senderId;
    }

    public NotificationTargetType getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public Instant getFireAt() {
        return fireAt;
    }
}
