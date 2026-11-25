package com.ject.exchange.application.service.command;

import com.ject.common.event.exchange.AppointmentCanceledByMemberWithdrawal;
import com.ject.exchange.domain.domain.appointment.Appointment;
import com.ject.exchange.domain.domain.appointment.AppointmentStatus;
import com.ject.exchange.domain.repository.AppointmentRepository;
import com.ject.exchange.domain.repository.PostLockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WithdrawnMemberAppointmentService {

    private static final EnumSet<AppointmentStatus> ACTIVE_STATUSES =
            EnumSet.of(AppointmentStatus.PROPOSED, AppointmentStatus.CONFIRMED);

    private final AppointmentRepository appointmentRepository;
    private final PostLockRepository postLockRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 탈퇴 회원이 참여한 진행 중(PROPOSED · CONFIRMED) 약속을 탈퇴 사유로 취소한다 (#139)
     * @param memberId 탈퇴한 회원 id
     * @param withdrawnAt 회원 탈퇴가 확정된 시각
     */
    @Transactional
    public void cancelActiveAppointments(Long memberId, Instant withdrawnAt) {
        List<Long> candidateIds = appointmentRepository.findIdsByMemberIdAndStatuses(memberId, ACTIVE_STATUSES);
        for (Long appointmentId : candidateIds) {
            cancelIfStillActive(appointmentId, memberId, withdrawnAt);
        }
    }

    /**
     * 글 잠금 → 약속 행 잠금 순서로 후보를 다시 읽어, 그 사이 상태가 바뀌었거나 대상이 아니면 건너뛴다 (application 규약 "동시 변경 보호")
     */
    private void cancelIfStillActive(Long appointmentId, Long memberId, Instant withdrawnAt) {
        List<Long> postIds = appointmentRepository.findPostIdsOf(appointmentId);
        postLockRepository.lockAll(postIds);
        Optional<Appointment> locked = appointmentRepository.findByIdForUpdate(appointmentId);
        if (locked.isEmpty()) {
            return;
        }
        Appointment appointment = locked.get();
        if (!isParticipant(appointment, memberId)) {
            return;
        }
        AppointmentStatus statusBeforeCancel = appointment.getStatus();
        if (!ACTIVE_STATUSES.contains(statusBeforeCancel)) {
            return;
        }
        Appointment canceled = appointment.cancelByMemberWithdrawal(memberId, withdrawnAt);
        appointmentRepository.save(canceled);
        // 예약중이던 글 정리는 CONFIRMED가 끊어질 때만 필요하므로 그때만 post용 이벤트를 발행한다 (AC-6)
        if (statusBeforeCancel == AppointmentStatus.CONFIRMED) {
            eventPublisher.publishEvent(
                    new AppointmentCanceledByMemberWithdrawal(appointmentId, postIds, memberId));
        }
    }

    private boolean isParticipant(Appointment appointment, Long memberId) {
        return appointment.getProposerUserId().equals(memberId)
                || appointment.getReceiverUserId().equals(memberId);
    }
}
