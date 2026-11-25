package com.ject.exchange.application.service.contract;

import com.ject.exchange.domain.domain.appointment.Appointment;
import com.ject.exchange.domain.domain.appointment.AppointmentStatus;
import com.ject.exchange.domain.repository.AppointmentRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** 확정 약속 수 조회 계약 테스트용 저장소. 저장된 약속의 참여자와 현재 상태로 직접 집계한다. */
class FakeAppointmentRepository implements AppointmentRepository {

    private final List<Appointment> appointments = new ArrayList<>();
    private int countCalls;

    @Override
    public int countConfirmedByMemberId(Long memberId) {
        countCalls++;
        return Math.toIntExact(appointments.stream()
                .filter(appointment -> appointment.getStatus() == AppointmentStatus.CONFIRMED)
                .filter(appointment -> appointment.getProposerUserId().equals(memberId)
                        || appointment.getReceiverUserId().equals(memberId))
                .count());
    }

    void preload(Appointment... appointments) {
        this.appointments.addAll(List.of(appointments));
    }

    int countCalls() {
        return countCalls;
    }

    @Override
    public Appointment save(Appointment appointment) {
        throw new UnsupportedOperationException("조회 계약은 약속을 저장하지 않는다");
    }

    @Override
    public Optional<Appointment> findById(Long id) {
        throw new UnsupportedOperationException("조회 계약은 약속 단건을 조회하지 않는다");
    }

    @Override
    public List<Appointment> findAllByExchangeId(Long exchangeId) {
        throw new UnsupportedOperationException("조회 계약은 교환별 약속을 조회하지 않는다");
    }

    @Override
    public boolean existsPendingProposalOnPost(Long postId, Long excludedExchangeId) {
        throw new UnsupportedOperationException("조회 계약은 진행 중 제안을 판정하지 않는다");
    }

    @Override
    public Optional<Appointment> findByIdForUpdate(Long id) {
        throw new UnsupportedOperationException("조회 계약은 약속을 잠그지 않는다");
    }

    @Override
    public List<Long> findPostIdsOf(Long appointmentId) {
        throw new UnsupportedOperationException("조회 계약은 글 ID를 조회하지 않는다");
    }

    @Override
    public Set<Long> findPostIdsBoundBy(Collection<Long> postIds, AppointmentStatus status, Long excludedExchangeId) {
        throw new UnsupportedOperationException("조회 계약은 묶인 글을 조회하지 않는다");
    }

    @Override
    public List<Appointment> findPendingProposalsInvolving(Collection<Long> postIds, Long excludedExchangeId) {
        throw new UnsupportedOperationException("조회 계약은 진행 중 제안을 조회하지 않는다");
    }

    @Override
    public List<Long> findIdsByMemberIdAndStatuses(Long memberId, Collection<AppointmentStatus> statuses) {
        throw new UnsupportedOperationException("조회 계약은 탈퇴 회원의 약속 ID를 조회하지 않는다");
    }
}
