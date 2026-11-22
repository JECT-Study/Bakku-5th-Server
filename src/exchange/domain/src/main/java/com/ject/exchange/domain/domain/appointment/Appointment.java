package com.ject.exchange.domain.domain.appointment;

import com.ject.exchange.domain.domain.appointment.vo.AppointmentItemIds;
import com.ject.exchange.domain.domain.appointment.vo.AppointmentTime;
import com.ject.exchange.domain.domain.appointment.vo.ConfirmationReply;
import com.ject.exchange.domain.domain.appointment.vo.Place;
import com.ject.exchange.domain.exception.ExchangeErrorCode;
import com.ject.exchange.domain.exception.ExchangeException;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Getter
public class Appointment {

    private static final Duration PROPOSAL_LIFETIME = Duration.ofHours(24);
    // 교환 확인은 약속 시각(지금 바로는 확정 시각) 30분 뒤에 열린다 (F-01-1)
    private static final Duration CONFIRMATION_DELAY = Duration.ofMinutes(30);
    // 못 만남 뒤 이유 남기기 기한. 자동 완료와 같은 기준인 질문 뒤 3일 (F-04-1, spec 결정 5)
    private static final Duration REPLY_DEADLINE = Duration.ofDays(3);

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
    private final Instant resolvedAt;
    private final List<ConfirmationReply> confirmationReplies;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Appointment(Long id, Long exchangeId, Long proposerUserId, Long receiverUserId, AppointmentStatus status,
                        AppointmentTime time, Place place, AppointmentItemIds itemIds, Instant expiresAt,
                        Integer proposerReminderOffsetMinutes, Integer receiverReminderOffsetMinutes,
                        Instant confirmedAt, Instant resolvedAt, List<ConfirmationReply> confirmationReplies,
                        Instant createdAt, Instant updatedAt) {
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
        this.resolvedAt = resolvedAt;
        this.confirmationReplies = List.copyOf(confirmationReplies);
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
                time, place, itemIds, expiresAt, reminder, null, null, null, List.of(), null, null);
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
        return of(id, exchangeId, proposerUserId, receiverUserId, status, time, place, itemIds, expiresAt,
                proposerReminderOffsetMinutes, receiverReminderOffsetMinutes, confirmedAt, null, List.of(),
                createdAt, updatedAt);
    }

    /**
     * 불러온 도메인 데이터 객체화 (교환 확인 결과 시각 · 응답 포함)
     */
    public static Appointment of(Long id, Long exchangeId, Long proposerUserId, Long receiverUserId,
                                 AppointmentStatus status, AppointmentTime time, Place place,
                                 AppointmentItemIds itemIds, Instant expiresAt,
                                 Integer proposerReminderOffsetMinutes, Integer receiverReminderOffsetMinutes,
                                 Instant confirmedAt, Instant resolvedAt, List<ConfirmationReply> confirmationReplies,
                                 Instant createdAt, Instant updatedAt) {
        return new Appointment(id, exchangeId, proposerUserId, receiverUserId, status, time, place, itemIds,
                expiresAt, proposerReminderOffsetMinutes, receiverReminderOffsetMinutes, confirmedAt, resolvedAt,
                confirmationReplies, createdAt, updatedAt);
    }

    /**
     * 받는 사람인지 확인한다. 제안자 본인이나 다른 사람이면 NOT_APPOINTMENT_RECEIVER
     */
    public void validateReceiver(Long userId) {
        if (!receiverUserId.equals(userId)) {
            throw new ExchangeException(ExchangeErrorCode.NOT_APPOINTMENT_RECEIVER);
        }
    }

    /**
     * 제안 수락. 받는 사람이 만료 전 제안을 수락하면 확정된다. 지금 바로 약속이면 알림 값은 무시하고 null로 둔다
     * @param accepterId 수락하는 회원 ID (받는 사람)
     * @param reminderOffsetMinutes 받는 사람의 약속 전 알림(분). null이면 끄기
     * @param now 수락 시각 (확정 시각)
     */
    public Appointment accept(Long accepterId, Integer reminderOffsetMinutes, Instant now) {
        validateReceiver(accepterId);
        validateAcceptableStatus();
        validateNotExpired(now);
        // 지금 바로 약속은 알림을 쓰지 않는다
        Integer receiverReminder = time.type() == AppointmentTimeType.NOW ? null : reminderOffsetMinutes;
        return new Appointment(id, exchangeId, proposerUserId, receiverUserId, AppointmentStatus.CONFIRMED,
                time, place, itemIds, expiresAt, proposerReminderOffsetMinutes, receiverReminder,
                now, resolvedAt, confirmationReplies, createdAt, updatedAt);
    }

    private void validateAcceptableStatus() {
        // 다른 방에서 먼저 확정돼 자동 거절된 제안은 "다른 분과 약속이 잡혀 있어요"로 안내한다 (E-05-1)
        if (status == AppointmentStatus.AUTO_REJECTED) {
            throw new ExchangeException(ExchangeErrorCode.POST_RESERVED_ELSEWHERE);
        }
        if (status != AppointmentStatus.PROPOSED) {
            throw new ExchangeException(ExchangeErrorCode.PROPOSAL_NOT_ACTIVE);
        }
    }

    private void validateNotExpired(Instant now) {
        if (isPendingProposalAt(now)) {
            return;
        }
        // 만료 시각은 약속 시각을 넘지 않으므로, 약속 시각이 지났으면 그 안내를 우선한다
        if (time.scheduledAt() != null && !time.scheduledAt().isAfter(now)) {
            throw new ExchangeException(ExchangeErrorCode.SCHEDULED_AT_PASSED);
        }
        throw new ExchangeException(ExchangeErrorCode.PROPOSAL_NOT_ACTIVE);
    }

    /**
     * 제안 거절. 받는 사람이 만료 전 제안을 거절하면 무효가 된다
     * @param rejecterId 거절하는 회원 ID (받는 사람)
     * @param now 거절 시각
     */
    public Appointment reject(Long rejecterId, Instant now) {
        validateReceiver(rejecterId);
        // 거절은 결과가 "무효"로 같아 끝난 상태(자동 거절 포함) · 만료를 모두 같은 코드로 안내한다 (spec 결정 2)
        if (!isPendingProposalAt(now)) {
            throw new ExchangeException(ExchangeErrorCode.PROPOSAL_NOT_ACTIVE);
        }
        return new Appointment(id, exchangeId, proposerUserId, receiverUserId, AppointmentStatus.REJECTED,
                time, place, itemIds, expiresAt, proposerReminderOffsetMinutes, receiverReminderOffsetMinutes,
                confirmedAt, resolvedAt, confirmationReplies, createdAt, updatedAt);
    }

    /**
     * 제안자인지 확인한다. 받는 사람이나 다른 사람이면 NOT_APPOINTMENT_PROPOSER
     */
    public void validateProposer(Long userId) {
        if (!proposerUserId.equals(userId)) {
            throw new ExchangeException(ExchangeErrorCode.NOT_APPOINTMENT_PROPOSER);
        }
    }

    /**
     * 제안 철회. 제안자가 만료 전 자기 제안을 철회하면 무효가 된다
     * @param withdrawerId 철회하는 회원 ID (제안자)
     * @param now 철회 시각
     */
    public Appointment withdraw(Long withdrawerId, Instant now) {
        validateProposer(withdrawerId);
        // 철회도 결과가 "무효"로 같아 끝난 상태(자동 거절 포함) · 만료를 모두 같은 코드로 안내한다 (spec 결정 2)
        if (!isPendingProposalAt(now)) {
            throw new ExchangeException(ExchangeErrorCode.PROPOSAL_NOT_ACTIVE);
        }
        return new Appointment(id, exchangeId, proposerUserId, receiverUserId, AppointmentStatus.WITHDRAWN,
                time, place, itemIds, expiresAt, proposerReminderOffsetMinutes, receiverReminderOffsetMinutes,
                confirmedAt, resolvedAt, confirmationReplies, createdAt, updatedAt);
    }

    /**
     * 내 약속 전 알림 변경. 확정 약속의 참여자가 자기 알림만 바꾼다. 알림 시각이 이미 지났어도 저장한다
     * @param memberId 바꾸는 회원 ID (제안자 또는 받는 사람)
     * @param reminderOffsetMinutes 내 약속 전 알림(분). null이면 끄기
     * @param now 변경 시각
     */
    public Appointment changeReminder(Long memberId, Integer reminderOffsetMinutes, Instant now) {
        boolean proposer = proposerUserId.equals(memberId);
        if (!proposer && !receiverUserId.equals(memberId)) {
            throw new ExchangeException(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT);
        }
        validateReminderChangeable(now);
        // 상대 설정에는 영향을 주지 않는다 (E-06-1 "내 알림만 설정돼요")
        Integer proposerReminder = proposer ? reminderOffsetMinutes : proposerReminderOffsetMinutes;
        Integer receiverReminder = proposer ? receiverReminderOffsetMinutes : reminderOffsetMinutes;
        return new Appointment(id, exchangeId, proposerUserId, receiverUserId, status,
                time, place, itemIds, expiresAt, proposerReminder, receiverReminder,
                confirmedAt, resolvedAt, confirmationReplies, createdAt, updatedAt);
    }

    private void validateReminderChangeable(Instant now) {
        if (status != AppointmentStatus.CONFIRMED) {
            throw new ExchangeException(ExchangeErrorCode.INVALID_APPOINTMENT_STATUS);
        }
        // 지금 바로 약속은 알림 설정이 숨겨져 있어 끄기 요청도 받지 않는다 (spec 결정 3)
        if (time.type() == AppointmentTimeType.NOW) {
            throw new ExchangeException(ExchangeErrorCode.REMINDER_NOT_ALLOWED);
        }
        if (!time.scheduledAt().isAfter(now)) {
            throw new ExchangeException(ExchangeErrorCode.SCHEDULED_AT_PASSED);
        }
    }

    /**
     * 교환 확인 응답. 확정 약속은 먼저 답한 사람 기준으로 교환 완료(DONE) 또는 못 만남(NOT_MET)이 되고,
     * 못 만남 뒤에는 상대가 기한 안에 자기 이유를 한 번 남긴다
     * @param memberId 응답하는 회원 ID (제안자 또는 받는 사람)
     * @param notMetReason 못 만난 이유. DONE이면 null
     * @param notMetDetail 직접 입력 (OTHER일 때만)
     * @param now 응답 시각
     */
    public Appointment answerConfirmation(Long memberId, ConfirmationAnswer answer, NotMetReason notMetReason,
                                          String notMetDetail, Instant now) {
        if (!proposerUserId.equals(memberId) && !receiverUserId.equals(memberId)) {
            throw new ExchangeException(ExchangeErrorCode.NOT_EXCHANGE_PARTICIPANT);
        }
        if (status != AppointmentStatus.CONFIRMED && status != AppointmentStatus.COMPLETED
                && status != AppointmentStatus.NOT_MET) {
            throw new ExchangeException(ExchangeErrorCode.INVALID_APPOINTMENT_STATUS);
        }
        // 응답은 1인 1회, 바꿀 수 없다 (F-01-1)
        if (replyOf(memberId).isPresent()) {
            throw new ExchangeException(ExchangeErrorCode.CONFIRMATION_ALREADY_ANSWERED);
        }
        ConfirmationReply reply = answer == ConfirmationAnswer.DONE
                ? new ConfirmationReply(memberId, answer, null, null, now)
                : new ConfirmationReply(memberId, answer, notMetReason, notMetDetail, now);
        if (status == AppointmentStatus.COMPLETED) {
            // 완료가 우선이라 상대의 못 만났어요도 처리하지 않는다 (F-01-1 "응답이 엇갈림")
            throw new ExchangeException(ExchangeErrorCode.CONFIRMATION_ALREADY_DECIDED);
        }
        if (status == AppointmentStatus.NOT_MET) {
            return leaveNotMetReason(answer, reply, now);
        }
        Instant opensAt = confirmationOpensAt();
        // 열림 시각을 정할 수 없는 데이터(지금 바로인데 확정 시각 없음)는 열리지 않은 것으로 본다
        if (opensAt == null || now.isBefore(opensAt)) {
            throw new ExchangeException(ExchangeErrorCode.CONFIRMATION_NOT_OPEN);
        }
        AppointmentStatus decided = answer == ConfirmationAnswer.DONE
                ? AppointmentStatus.COMPLETED
                : AppointmentStatus.NOT_MET;
        return new Appointment(id, exchangeId, proposerUserId, receiverUserId, decided,
                time, place, itemIds, expiresAt, proposerReminderOffsetMinutes, receiverReminderOffsetMinutes,
                confirmedAt, now, withReply(reply), createdAt, updatedAt);
    }

    /**
     * 못 만남으로 정해진 뒤 상대가 자기 이유를 남긴다. 결과는 바뀌지 않는다 (F-04-1 동작 3)
     */
    private Appointment leaveNotMetReason(ConfirmationAnswer answer, ConfirmationReply reply, Instant now) {
        // 상대 카드에는 [이유 남기기]만 있다. 기한이 지나면 카드가 닫힌다 (spec 결정 4 · 5)
        Instant opensAt = confirmationOpensAt();
        if (answer == ConfirmationAnswer.DONE || opensAt == null || !now.isBefore(opensAt.plus(REPLY_DEADLINE))) {
            throw new ExchangeException(ExchangeErrorCode.CONFIRMATION_ALREADY_DECIDED);
        }
        return new Appointment(id, exchangeId, proposerUserId, receiverUserId, status,
                time, place, itemIds, expiresAt, proposerReminderOffsetMinutes, receiverReminderOffsetMinutes,
                confirmedAt, resolvedAt, withReply(reply), createdAt, updatedAt);
    }

    private List<ConfirmationReply> withReply(ConfirmationReply reply) {
        List<ConfirmationReply> replies = new ArrayList<>(confirmationReplies);
        replies.add(reply);
        return replies;
    }

    /**
     * 교환 확인이 열리는 시각. 확정된 적 없는 약속이면 null, 지금 바로는 확정 시각 + 30분, 그 외는 약속 시각 + 30분
     */
    public Instant confirmationOpensAt() {
        if (!wasConfirmed()) {
            return null;
        }
        // 지금 바로 약속은 약속 시각이 없어 확정 시각을 기준으로 한다 (F-01-1 진입, 11/16)
        Instant base = time.type() == AppointmentTimeType.NOW ? confirmedAt : time.scheduledAt();
        return base == null ? null : base.plus(CONFIRMATION_DELAY);
    }

    private boolean wasConfirmed() {
        return status == AppointmentStatus.CONFIRMED || status == AppointmentStatus.COMPLETED
                || status == AppointmentStatus.NOT_MET || status == AppointmentStatus.CANCELED;
    }

    /**
     * 회원의 교환 확인 응답
     */
    public Optional<ConfirmationReply> replyOf(Long userId) {
        return confirmationReplies.stream()
                .filter(reply -> reply.userId().equals(userId))
                .findFirst();
    }

    /**
     * 같은 글(또는 고른 첨부 글)의 다른 약속이 확정되어 자동 거절된 제안
     */
    public Appointment autoReject() {
        return new Appointment(id, exchangeId, proposerUserId, receiverUserId, AppointmentStatus.AUTO_REJECTED,
                time, place, itemIds, expiresAt, proposerReminderOffsetMinutes, receiverReminderOffsetMinutes,
                confirmedAt, resolvedAt, confirmationReplies, createdAt, updatedAt);
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
