package com.ject.exchange.domain.domain.appointment;

import com.ject.exchange.domain.domain.appointment.vo.AppointmentItemIds;
import com.ject.exchange.domain.domain.appointment.vo.AppointmentTime;
import com.ject.exchange.domain.domain.appointment.vo.Place;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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
     * 한 교환의 약속 중 현재 유효한 약속 (확정됐거나 만료 전 제안). 둘 이상이면 확정을 먼저, 같은 상태면 나중에 만든 약속
     * @param appointments 같은 교환의 약속들
     * @param now 만료 판단 기준 시각
     */
    public static Optional<Appointment> currentAmong(List<Appointment> appointments, Instant now) {
        // 정상 흐름에서는 후보가 하나뿐이다. 데이터가 어긋나도 확정 약속이 화면(약속 취소 메뉴)에 우선 반영되도록 정렬한다
        Comparator<Appointment> priority = Comparator
                .comparing((Appointment appointment) -> appointment.getStatus() == AppointmentStatus.CONFIRMED)
                .thenComparing(Appointment::getId, Comparator.nullsFirst(Comparator.naturalOrder()));
        return appointments.stream()
                .filter(appointment -> appointment.isInProgressAt(now))
                .max(priority);
    }

    /**
     * 수락을 기다리는 유효한 제안인지 (제안 상태이고 만료 시각 전)
     */
    public boolean isPendingProposalAt(Instant now) {
        return status == AppointmentStatus.PROPOSED && expiresAt.isAfter(now);
    }

    /**
     * 조회 시점에 보여 줄 상태. 만료 스케줄이 없어 저장 상태가 PROPOSED로 남은 제안은 만료 시각이 지나면 EXPIRED로 본다.
     * 저장값은 바꾸지 않는다
     */
    public AppointmentStatus displayStatusAt(Instant now) {
        if (status == AppointmentStatus.PROPOSED && !isPendingProposalAt(now)) {
            return AppointmentStatus.EXPIRED;
        }
        return status;
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
