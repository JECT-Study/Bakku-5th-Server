package com.ject.exchange.domain.domain.appointment;

import com.ject.exchange.domain.domain.appointment.vo.AppointmentItemIds;
import com.ject.exchange.domain.domain.appointment.vo.AppointmentTime;
import com.ject.exchange.domain.domain.appointment.vo.Place;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;

@Getter
public class Appointment {

    private static final Duration PROPOSAL_LIFETIME = Duration.ofHours(24);

    private final Long id;
    private final Long exchangeId;
    private final Long proposerUserId;
    private final Long receiverUserId;
    private final AppointmentStatus status;
    private final AppointmentTime time;
    private final Place place;
    private final AppointmentItemIds itemIds;
    private final Instant expiresAt;
    private final Integer proposerReminderOffsetMinutes;
    private final Integer receiverReminderOffsetMinutes;
    private final Instant confirmedAt;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Appointment(Long id, Long exchangeId, Long proposerUserId, Long receiverUserId, AppointmentStatus status,
                        AppointmentTime time, Place place, AppointmentItemIds itemIds, Instant expiresAt,
                        Integer proposerReminderOffsetMinutes, Integer receiverReminderOffsetMinutes,
                        Instant confirmedAt, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.exchangeId = exchangeId;
        this.proposerUserId = proposerUserId;
        this.receiverUserId = receiverUserId;
        this.status = status;
        this.time = time;
        this.place = place;
        this.itemIds = itemIds;
        this.expiresAt = expiresAt;
        this.proposerReminderOffsetMinutes = proposerReminderOffsetMinutes;
        this.receiverReminderOffsetMinutes = receiverReminderOffsetMinutes;
        this.confirmedAt = confirmedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 약속 제안. 만료 시각은 제안 시각 + 24시간과 약속 시각 중 빠른 쪽, 지금 바로면 제안 시각 + 24시간
     * @param proposerReminderOffsetMinutes 제안자의 약속 전 알림(분). 지금 바로면 무시하고 null로 둔다
     * @param now 제안 시각
     */
    public static Appointment propose(Long exchangeId, Long proposerUserId, Long receiverUserId, AppointmentTime time,
                                      Place place, AppointmentItemIds itemIds, Integer proposerReminderOffsetMinutes,
                                      Instant now) {
        Instant expiresAt = expiresAtOf(time, now);
        // 지금 바로 약속은 알림을 쓰지 않는다
        Integer reminder = time.type() == AppointmentTimeType.NOW ? null : proposerReminderOffsetMinutes;
        return new Appointment(null, exchangeId, proposerUserId, receiverUserId, AppointmentStatus.PROPOSED,
                time, place, itemIds, expiresAt, reminder, null, null, null, null);
    }

    private static Instant expiresAtOf(AppointmentTime time, Instant now) {
        Instant dayLater = now.plus(PROPOSAL_LIFETIME);
        if (time.scheduledAt() == null || dayLater.isBefore(time.scheduledAt())) {
            return dayLater;
        }
        return time.scheduledAt();
    }

    /**
     * 불러온 도메인 데이터 객체화
     */
    public static Appointment of(Long id, Long exchangeId, Long proposerUserId, Long receiverUserId,
                                 AppointmentStatus status, AppointmentTime time, Place place,
                                 AppointmentItemIds itemIds, Instant expiresAt,
                                 Integer proposerReminderOffsetMinutes, Integer receiverReminderOffsetMinutes,
                                 Instant confirmedAt, Instant createdAt, Instant updatedAt) {
        return new Appointment(id, exchangeId, proposerUserId, receiverUserId, status, time, place, itemIds,
                expiresAt, proposerReminderOffsetMinutes, receiverReminderOffsetMinutes, confirmedAt, createdAt, updatedAt);
    }

    /**
     * 진행 중인 약속인지. 확정됐거나, 제안 상태이고 만료 시각 전이면 진행 중이다 (만료 스케줄이 없어 시각으로 판단)
     */
    public boolean isInProgressAt(Instant now) {
        return status == AppointmentStatus.CONFIRMED || isPendingProposalAt(now);
    }

    /**
     * 수락을 기다리는 유효한 제안인지 (제안 상태이고 만료 시각 전)
     */
    public boolean isPendingProposalAt(Instant now) {
        return status == AppointmentStatus.PROPOSED && expiresAt.isAfter(now);
    }

    /**
     * 조회자의 약속 전 알림(분). 참여자가 아니면 null
     */
    public Integer reminderOffsetMinutesOf(Long userId) {
        if (proposerUserId.equals(userId)) {
            return proposerReminderOffsetMinutes;
        }
        if (receiverUserId.equals(userId)) {
            return receiverReminderOffsetMinutes;
        }
        return null;
    }
}
